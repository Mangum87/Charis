package com.charis;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.charis.data.Distribution;
import com.charis.data.Item;
import com.charis.data.NonSellableItem;
import com.charis.data.SellableItem;
import com.charis.util.Database;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import static com.charis.Distribution.SALES_TAX;

public class ReportActivity extends AppCompatActivity
{
    private Database database;
    private TableLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        this.database = new Database();
        this.layout = findViewById(R.id.tableLayout);

        setComponents();
    }


    /**
     * Set up the UI components.
     */
    private void setComponents()
    {
        // Set spinner
        Spinner spin = findViewById(R.id.spinReport);
        String[] reports = {"Sales Tax", "Inventory Age", "Stock Report", "Item Outflow"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, reports);
        spin.setAdapter(adapter);
        spin.setSelection(0); // Set to default value

        Spinner spin2 = findViewById(R.id.spinMonth);
        String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months);
        spin2.setAdapter(adapter2);
        spin2.setSelection(0); // Set to default value

        Spinner spin3 = findViewById(R.id.spinYear);
        String[] years = {"2020", "2021", "2022"};
        ArrayAdapter<String> adapter3 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years);
        spin3.setAdapter(adapter3);
        spin3.setSelection(0); // Set to default value

        // Set spinner listener
        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if(position == 0 || position == 3) // Require month/year
                {
                    findViewById(R.id.spinMonth).setEnabled(true);
                    findViewById(R.id.spinYear).setEnabled(true);
                }
                else // Don't require month/year
                {
                    findViewById(R.id.spinMonth).setEnabled(false);
                    findViewById(R.id.spinYear).setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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
     * Row is added to the table with
     * the given row of data.
     * @param data Row of data values
     */
    private void addRowToTable(String[] data)
    {
        if(data == null)
            return;

        TableRow row = new TableRow(this);
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
     * Run the selected report.
     * @param view
     */
    public void runReport(View view)
    {
        ((TableLayout)findViewById(R.id.tableLayout)).removeAllViews();
        int pos = ((Spinner)findViewById(R.id.spinReport)).getSelectedItemPosition();

        switch(pos)
        {
            case 0: // Sales Tax
                salesTax();
                break;
            case 1: // Inventory Age
                inventoryAge();
                break;
            case 2: // Stock Report
                stockReport();
                break;
            case 3: // Outflow
                outFlow();
                break;
        }
    }


    /**
     * Performs a sales tax report on
     * sold goods for selected month
     * and year.
     */
    private void salesTax()
    {
        // Get selected month
        int month = ((Spinner)findViewById(R.id.spinMonth)).getSelectedItemPosition();

        // Get selected year
        int year = ((Spinner)findViewById(R.id.spinYear)).getSelectedItemPosition();

        // Get sales records for month
        int yearOffset = Integer.parseInt((String)((Spinner)findViewById(R.id.spinYear)).getSelectedItem());
        Distribution[] dists = this.database.getDistributionsByDate(month, year + yearOffset);

        // Make column header
        String[] col = {"# of Sales", "Total Sales", "Tax Total", "Gross Total"};
        addRowToTable(col);

        // Calculate totals
        // gross(1 + tax%) = total
        double total = calcDistTotal(dists);
        double gross = total/(1 + SALES_TAX);
        double tax = total - gross;

        int saleCount = calcSalesCount(dists);

        // Fill table
        String[] values = {String.valueOf(saleCount), String.format("$%.2f", total), String.format("$%.2f", tax), String.format("$%.2f", gross)};
        addRowToTable(values);
    }


    /**
     * Returns the number of sales with
     * a price above 0.0;
     * @param dists Array of sales
     * @return Count of sales
     */
    private int calcSalesCount(Distribution[] dists)
    {
        int total = 0;

        if(dists != null)
        {
            for(int i = 0; i < dists.length; i++)
            {
                if(dists[i].getAmount() > 0.0)
                    total++;
            }
        }

        return total;
    }


    /**
     * Calculates the total sales from an
     * array of distributions.
     * @param dists Array of distributions
     * @return Total sales amount
     */
    private double calcDistTotal(Distribution[] dists)
    {
        double total = 0.0;

        if(dists != null)
        {
            for(int i = 0; i < dists.length; i++)
            {
                total += dists[i].getAmount();
            }
        }

        return total;
    }


    /**
     * Displays an inventory report of
     * sellable items by received date.
     */
    private void inventoryAge()
    {
        // Get sellable items
        SellableItem[] sell = this.database.getAllSellable();
        DateFormat format = new SimpleDateFormat("MM/dd/yyyy");

        String[] col = {"ID", "Name", "Quantity", "Received"};
        addRowToTable(col);

        if(sell != null)
        {
            Arrays.sort(sell); // Order by received date

            for(int i = 0; i < sell.length; i++)
            {
                String[] sellRow = {sell[i].getID(), sell[i].getDescription(), String.valueOf(sell[i].getQuantity()), format.format(sell[i].getReceived())};
                addRowToTable(sellRow);
            }
        }
    }


    /**
     * Create an inventory report of all items.
     */
    private void stockReport()
    {
        SellableItem[] sell = this.database.getAllSellable(); // Get all the sellables
        NonSellableItem[] nonSell = this.database.getAllNonSellable(); // Get all the nonsellables

        String[] col = {"ID", "Name", "Quantity"};
        addRowToTable(col);

        if(sell != null)
        {
            String[] sellCol = {"For Sale", "----", "----"};
            addRowToTable(sellCol);

            for (int i = 0; i < sell.length; i++)
            {
                String[] sellRow = {sell[i].getID(), sell[i].getDescription(), String.valueOf(sell[i].getQuantity())};
                addRowToTable(sellRow);
            }
        }

        if(nonSell != null)
        {
            String[] nonSellCol = {"For Free", "----", "----"};
            addRowToTable(nonSellCol);

            for(int i = 0; i < nonSell.length; i++)
            {
                String[] nonSellRow = {nonSell[i].getID(), nonSell[i].getDescription(), String.valueOf(nonSell[i].getQuantity())};
                addRowToTable(nonSellRow);
            }
        }
    }


    /**
     * For all items, calculate the outflow
     * for a give month.
     */
    private void outFlow()
    {
        // Get selected month
        int month = ((Spinner)findViewById(R.id.spinMonth)).getSelectedItemPosition();

        // Get selected year
        int year = ((Spinner)findViewById(R.id.spinYear)).getSelectedItemPosition();

        // Get sales records for month
        int yearOffset = Integer.parseInt((String)((Spinner)findViewById(R.id.spinYear)).getSelectedItem());
        HashMap<String, Item>[] maps = this.database.getDistItemCountByDate(month, year + yearOffset);

        // Make column header
        String[] col = {"ID", "Name", "Quantity", "Value"};
        addRowToTable(col);

        String[] sellCol = {"For Sale", "----", "----", "----"};
        addRowToTable(sellCol);
        fillTableMap(maps[0]);

        String[] nonSellCol = {"For Free", "----", "----", "----"};
        addRowToTable(nonSellCol);
        fillTableMap(maps[1]);
    }


    /**
     * Fills the table with the given map information for outFlow().
     * @param map Map of data
     */
    private void fillTableMap(HashMap<String, Item> map)
    {
        Collection<Item> c = map.values(); // Get values

        for (Item i : c)
        {
            String quant = String.valueOf(i.getQuantity());
            String value = String.format("$%.2f", (i.getQuantity() * i.getPrice()));
            String[] col = {i.getID(), i.getDescription(), quant, value};
            addRowToTable(col);
        }
    }


    /**
     * Close the database connection and
     * the form.
     * @param view
     */
    public void closeForm(View view)
    {
        this.database.close();
        this.finish();
    }
}
