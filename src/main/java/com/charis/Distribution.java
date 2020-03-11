package com.charis;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.charis.data.Category;
import com.charis.data.Enum.Condition;
import com.charis.data.Item;
import com.charis.data.Location;
import com.charis.data.NonSellableItem;
import com.charis.data.SellableItem;
import com.charis.data.User;
import com.charis.util.Database;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Date;

public class Distribution extends AppCompatActivity
{
    private Database db;
    private ArrayList<Item> list; // Holds list of items in table
    private TableLayout layout;
    private TextWatcher watcher; // For EditText quantity
    private View.OnClickListener listener; // Listener for table rows
    private int viewIndex;  // Index of currently selected row
    private User user;

    /**
     * Sales tax used to calculate price.
     */
    public final static double SALES_TAX = 0.0825;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distribution);

        this.db = new Database(); // Get instance of database
        this.user = (User)getIntent().getSerializableExtra("user");
        this.layout = (TableLayout)findViewById(R.id.tablelayout);
        this.viewIndex = -1;
        this.list = new ArrayList<Item>(20);
        setHeader(); // Create the table header
        makeListeners(); // Create component listeners
    }


    /**
     * Create listeners for activity components to use.
     */
    private void makeListeners()
    {
        this.watcher = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                updatePrice(); // Update price
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        this.listener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                handleRowClick(v);
            }
        };

        ((TextView)findViewById(R.id.txtBarcode)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if(s.length() == 13)
                {
                    handleBarcode(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }


    private void handleRowClick(View v)
    {
        this.viewIndex = findViewIndex(v); // Get index of clicked row
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
     * Handles the event when a barcode is
     * entered.
     * @param id ID of item
     */
    private void handleBarcode(String id)
    {
        NonSellableItem nonSell = db.getNonSellableItem(id); // Look for item
        SellableItem sell = db.getSellableItem(id);

        if(sell != null)
        {
            if(nonSell != null) // Sell and nonSell exist
            {
                if(sell.getQuantity() > 0)
                {
                    if(nonSell.getQuantity() > 0)
                        showDialog(sell, nonSell); // Both have stock
                    else
                        addToTable(sell); // Only sell has stock
                }
                else
                {
                    if(nonSell.getQuantity() > 0)
                        addToTable(nonSell); // Only nonSell has stock
                }
            }
            else // Only sell exists
                addToTable(sell); // Launch for sell
        }
        else
        {
            if(nonSell != null) // Only nonSell exists
            {
                addToTable(nonSell); // Launch for nonSell
            }
        }


        updatePrice(); // Update totals

        // Reset TextView
        ((TextView)findViewById(R.id.txtBarcode)).setText("");
        findViewById(R.id.txtBarcode).requestFocus();
    }


    /**
     * Shows the dialog for allowing a
     * user to select a certain item type.
     * i.e. Sellable or NonSellable items.
     * @param i1 Sellable item to choose from
     * @param i2 NonSellable item to choose from
     */
    private void showDialog(final SellableItem i1, final NonSellableItem i2)
    {
        final String[] list = {"Sold items", "Free items"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Take from");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setItems(list, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch(which)
                {
                    case 0:
                        addToTable(i1);
                        break;
                    case 1:
                        addToTable(i2);
                        break;
                }

                updatePrice();
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * Sets the header for the table layout.
     */
    private void setHeader()
    {
        TableLayout headerLayout = (TableLayout)findViewById(R.id.headerlayout);
        // Set up header row
        TableRow header = new TableRow(this);
        TableRow.LayoutParams p = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        header.setLayoutParams(p);

        // Set header columns
        TextView h1 = new TextView(this);
        textViewParams(h1, "Name");
        header.addView(h1);

        TextView h2 = new TextView(this);
        textViewParams(h2, "Price");
        header.addView(h2);

        TextView h3 = new TextView(this);
        textViewParams(h3, "Quantity");
        header.addView(h3);

        headerLayout.addView(header);
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
     * Add row to table with item information.
     * @param item Item to add
     */
    private void addRow(Item item)
    {
        // Set up header row
        TableRow row = new TableRow(this);
        row.setClickable(true);
        TableRow.LayoutParams p = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(p);
        row.setOnClickListener(this.listener);

        // Set header columns
        TextView h1 = new TextView(this);
        textViewParams(h1, item.getDescription());
        row.addView(h1);

        TextView h2 = new TextView(this);
        textViewParams(h2, String.format("%.2f", item.getPrice()));
        row.addView(h2);

        EditText h3 = new EditText(this);
        textViewParams(h3, "1");
        h3.addTextChangedListener(this.watcher);
        row.addView(h3);

        layout.addView(row); // Add to table
        this.list.add(item); // Add to list of items in table
    }


    /**
     * Adds item to the table. If the item is already in
     * the table, the quantity will increment, otherwise,
     * a new record is added.
     * @param item Item to add
     */
    private void addToTable(Item item)
    {
        int index = isItemInTable(item);

        if(index == -1) // Add to table
            addRow(item);
        else
            incrementItem(item, index); // Increment quantity
    }


    /**
     * Update price from table.
     */
    private void updatePrice()
    {
        double total = 0.0; // Running gross total


        // Variables for loop
        int quantity;
        double price;
        for(int i = 0; i < this.list.size(); i++)
        {
            TableRow row = (TableRow)layout.getChildAt(i); // Get row
            TextView colQuan = (TextView)row.getChildAt(2); // Get quantity column
            TextView colPrice = (TextView)row.getChildAt(1); // Get price column

            try { quantity = Integer.parseInt(colQuan.getText().toString()); }
            catch(NumberFormatException e) { quantity = 0; }

            price = Double.parseDouble(colPrice.getText().toString());

            total += price * quantity; // Calculate gross of one row
        }

        // Update activity
        ((TextView)findViewById(R.id.txtSubTotal)).setText(String.format("$%.2f", total)); // Update subtotal
        ((TextView)findViewById(R.id.txtTax)).setText(String.format("$%.2f", (total * SALES_TAX))); // Update tax
        ((TextView)findViewById(R.id.txtTotal)).setText(String.format("$%.2f", total*(1 + SALES_TAX))); // Update total
    }


    /**
     * Checks the items in the table to see if
     * the passed item is already added. If
     * returned index is -1, the item was not
     * found, otherwise, the index of the item
     * in the table is returned.
     * @param item Item to check
     * @return Index of item or -1 if not found
     */
    private int isItemInTable(Item item)
    {
        Item listItem;

        for(int i = 0; i < this.list.size(); i++)
        {
            listItem = this.list.get(i);

            if(listItem.getID().equals(item.getID()) && listItem.getPrice() == item.getPrice())
                return i;
        }

        return -1;
    }


    /**
     * Increment item quantity in the table at
     *  the given index. Index can be found using
     *  isItemInTable().
     * @param item Item to increment
     * @param index Index of item
     */
    private void incrementItem(Item item, int index)
    {
        TableRow row = (TableRow)layout.getChildAt(index); // Get row
        TextView col = (TextView)row.getChildAt(2); // Get quantity column
        int quantity = Integer.parseInt(col.getText().toString());
        col.setText(String.valueOf(quantity + 1)); // Set higher quantity
    }


    /**
     * Distribute the items in the table
     * and save changes to the database.
     * @param view
     */
    public void checkoutItems(View view)
    {
        if(this.list.size() == 0) // Table has to have something
            return;

        ArrayList<SellableItem> sellItem = new ArrayList<SellableItem>(this.list.size());
        ArrayList sellCount = new ArrayList(this.list.size());

        ArrayList<NonSellableItem> nonsellItem = new ArrayList<NonSellableItem>(this.list.size());
        ArrayList nonsellCount = new ArrayList(this.list.size());


        for(int i = 0; i < this.list.size(); i++)
        {
            // Get row info
            TableRow row = (TableRow)this.layout.getChildAt(i);
            TextView colPrice = (TextView)row.getChildAt(1);
            TextView colQuantity = (TextView)row.getChildAt(2);

            // Translate to vars
            double price = Double.valueOf(colPrice.getText().toString());
            int quantity = Integer.valueOf(colQuantity.getText().toString());

            if(price == 0.0) // 0.0 = NonSellableItem
            {
                nonsellItem.add((NonSellableItem) this.list.get(i));
                nonsellCount.add(quantity);
            }
            else // SellableItem
            {
                sellItem.add((SellableItem) this.list.get(i));
                sellCount.add(quantity);
            }
        }

        // Set up from table to send to database
        String strTotal = ((TextView)findViewById(R.id.txtTotal)).getText().toString().substring(1); // Skip over $
        double total = Double.valueOf(strTotal);
        SellableItem[] i1 = toArrayS(sellItem.toArray());
        int[] c1 = toArrayI(sellCount.toArray());
        NonSellableItem[] i2 = toArrayNS(nonsellItem.toArray());
        int[] c2 = toArrayI(nonsellCount.toArray());

        boolean suc = this.db.createDistItemRelation(i1, c1, i2, c2, total, new Date(), this.user);

        // Reset frame components
        if(suc)
        {
            this.layout.removeAllViews(); // Reset table to empty
            this.list = new ArrayList<Item>(20); // New list
            ((TextView)findViewById(R.id.txtBarcode)).requestFocus();
            ((TextView)findViewById(R.id.txtTax)).setText("$0.00");
            ((TextView)findViewById(R.id.txtTotal)).setText("$0.00");
            ((TextView)findViewById(R.id.txtSubTotal)).setText("$0.00");
        }
    }


    /**
     * Convert an Integer array to an
     * int array.
     * @param arr Array to convert
     * @return int array
     */
    private int[] toArrayI(Object[] arr)
    {
        int[] array = new int[arr.length];

        if(arr.length > 0)
        {
            for(int i = 0; i < arr.length; i++)
            {
                array[i] = ((Integer)arr[i]).intValue();
            }
        }

        return array;
    }


    /**
     * Convert the given object array
     * to a SellableItem array.
     * @param o Array to convert
     * @return Array of SellableItems
     */
    private SellableItem[] toArrayS(Object[] o)
    {
        SellableItem[] item = new SellableItem[o.length];

        for(int i = 0; i < item.length; i++)
        {
            item[i] = (SellableItem)o[i];
        }

        return item;
    }


    /**
     * Convert the given object array
     * to a NonSellableItem array.
     * @param o Array to convert
     * @return Array to NonSellableItems
     */
    private NonSellableItem[] toArrayNS(Object[] o)
    {
        NonSellableItem[] item = new NonSellableItem[o.length];

        for(int i = 0; i < item.length; i++)
        {
            item[i] = (NonSellableItem)o[i];
        }

        return item;
    }


    /**
     * Delete the selected item from the table.
     * @param v Row to delete
     */
    public void deleteItem(View v)
    {
        if(this.viewIndex > -1)
        {
            layout.removeView(v);
            list.remove(this.viewIndex);
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
        for(int i = 0; i < list.size(); i++)
        {
            if(layout.getChildAt(i) == v)
                return i;
        }

        return -1;
    }


    /**
     * Close the activity
     * @param view
     */
    public void closeForm(View view)
    {
        this.db.close();
        this.finish();
    }
}
