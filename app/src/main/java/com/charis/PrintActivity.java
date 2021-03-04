package com.charis;

import androidx.appcompat.app.AppCompatActivity;
import androidx.print.PrintHelper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.charis.data.Kit;
import com.charis.util.Database;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class PrintActivity extends AppCompatActivity
{
    private Database db;
    private TableLayout layout;
    private int viewIndex;  // Index of currently selected row
    private View.OnClickListener listener; // Listener for table rows
    private static final int SIZE = 200; // Size for bitmap

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);

        this.db = new Database();
        this.layout = findViewById(R.id.tableLayout);
        this.viewIndex = -1;
        findViewById(R.id.btnPrint).setEnabled(false);

        makeListeners();
        setTable(); // Set table data
    }


    /**
     * Set table information from database.
     */
    private void setTable()
    {
        HashMap<String, String> items = this.db.getUniqueItems();  // Get items
        Kit[] kits = this.db.getAllKits(); // Get kits

        // Fill table with all barcode stuff
        if(items != null)
        {
            // Create header for items
            String[] header = {"ID", "Name"};
            addRow(header, false);
            String[] it = {"Items", "-----"};
            addRow(it, false);

            Set<String> set = items.keySet(); // Set of keys
            Iterator<String> iter = set.iterator();
            String key, val;

            while(iter.hasNext())
            {
                key = iter.next();
                val = items.get(key);
                String[] row = {key, val};
                addRow(row, true);
            }
        }


        // Fill table with kits
        if(kits.length > 0)
        {
            // Create header for kits
            String[] header = {"Kits", "-----"};
            addRow(header, false);

            String key, val;

            for(Kit k : kits)
            {
                key = k.getID();
                val = k.getName();
                String[] row = {key, val};
                addRow(row, true);
            }
        }
    }


    /**
     * Set the listeners for the components.
     */
    private void makeListeners()
    {
        this.listener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                handleRowClick(v);
            }
        };
    }


    /**
     * Print the selected item.
     * @param view
     */
    public void printBarcode(View view)
    {
        String id = getID();

        if(id != null)
        {
            Bitmap map = makeBarcode(id);

            if(map != null)
            {
                print(map);
                this.viewIndex = -1;
                colorRows();
                findViewById(R.id.btnPrint).setEnabled(false); // Turn button off
            }
            else
                Toast.makeText(this, "Problem making barcode", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(this, "Problem finding ID", Toast.LENGTH_SHORT).show();
    }


    /**
     * Get the ID of the selected row.
     * Returns null if row is not selected.
     * @return ID of row or null
     */
    private String getID()
    {
        if(this.viewIndex < 0)
            return null;

        TableRow row = (TableRow)layout.getChildAt(viewIndex);
        TextView view = (TextView)row.getChildAt(0);
        String id = view.getText().toString();

        return id;
    }


    /**
     * Translate the given text to a QR code.
     * Returns null if text has length 0 or
     * process fails.
     * @param text
     * @return Bitmap of text or null
     */
    private Bitmap makeBarcode(String text)
    {
        if(text == null || text.length() == 0)
            return null;

        try
        {
            QRCodeWriter write = new QRCodeWriter();
            BitMatrix matrix = write.encode(text, BarcodeFormat.QR_CODE, SIZE, SIZE);
            Bitmap map = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);

            for(int i = 0; i < SIZE; i++)
            {
                for(int j = 0; j < SIZE; j++)
                {
                    map.setPixel(i, j, matrix.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }

            return map;
        }
        catch (WriterException e) { e.printStackTrace(); }

        return null;
    }


    /**
     * Print given bitmap to connected printers.
     * @param map Bitmap to print
     */
    private void print(Bitmap map)
    {
        PrintHelper printer = new PrintHelper(this);
        printer.setColorMode(PrintHelper.COLOR_MODE_MONOCHROME);
        printer.setOrientation(PrintHelper.ORIENTATION_PORTRAIT);
        printer.setScaleMode(PrintHelper.SCALE_MODE_FIT);

        printer.printBitmap("Barcode", map);
    }


    /**
     * Row is added to the table with
     * the given row of data.
     * @param data Row of data values
     * @param header True if non-clickable header
     */
    private void addRow(String[] data, boolean header)
    {
        if(data == null)
            return;

        TableRow row = new TableRow(this);

        if(header)
        {
            row.setClickable(true);
            row.setOnClickListener(this.listener);
        }

        TableRow.LayoutParams p = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
        row.setLayoutParams(p);

        for(int i = 0; i < data.length; i++)
        {
            TextView h1 = new TextView(this);
            textViewParams(h1, data[i]);
            row.addView(h1);
        }

        this.layout.addView(row);
    }


    /**
     * Adds parameters to given TextView.
     * @param v View to set up
     */
    private void textViewParams(TextView v, String name)
    {
        v.setText(name);
        v.setTextSize(17);
        v.setPadding(15, 12, 15, 12);
        v.setTextColor(Color.BLACK);
        v.setGravity(Gravity.CENTER_HORIZONTAL);
    }


    /**
     * Handle the row click event.
     * @param v
     */
    private void handleRowClick(View v)
    {
        this.viewIndex = findViewIndex(v); // Get index of clicked row
        findViewById(R.id.btnPrint).setEnabled(true); // Turn button on
        colorRows();
    }


    /**
     * Colors the selected row in the table.
     */
    private void colorRows()
    {
        TableRow row;

        for(int i = 0; i < layout.getChildCount(); i++)
        {
            row = (TableRow)this.layout.getChildAt(i);
            if (i == this.viewIndex)
                row.setBackgroundColor(Color.LTGRAY);
            else
                row.setBackgroundColor(Color.parseColor("#EEEEEE"));
        }
    }


    /**
     * Search the table to find the index
     * of the given View.
     * @param v View to find
     * @return Index of view or -1 if not found
     */
    private int findViewIndex(View v)
    {
        for(int i = 0; i < layout.getChildCount(); i++)
        {
            if(layout.getChildAt(i) == v)
                return i;
        }

        return -1;
    }



    /**
     * Close the form.
     * @param view
     */
    public void closeForm(View view)
    {
        this.db.close();
        this.finish();
    }
}
