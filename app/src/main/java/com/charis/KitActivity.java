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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.charis.data.Item;
import com.charis.data.Kit;
import com.charis.data.NonSellableItem;
import com.charis.data.SellableItem;
import com.charis.util.Database;

import java.util.ArrayList;

public class KitActivity extends AppCompatActivity
{
    private Database database;
    private ArrayList<Item> list; // Holds list of items in table
    private TableLayout layout;
    private View.OnClickListener listener; // Listener for table rows
    private CompoundButton.OnCheckedChangeListener checkListener;
    private Kit[] kits;
    private Kit currentKit;
    private int viewIndex;  // Index of currently selected row


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kit);

        // Set variables
        this.database = new Database(); // Get instance
        this.viewIndex = -1;
        this.layout = findViewById(R.id.tablelayout);
        this.currentKit = null;
        this.list = new ArrayList<Item>(15);

        setComponents();
    }


    /**
     * Set the UI components of the screen.
     */
    private void setComponents()
    {
        setHeader();
        fillSpinner();
        setListeners();

        findViewById(R.id.btnDelete).setEnabled(false);
    }


    /**
     * Set up the component listeners.
     */
    private void setListeners()
    {
        // Row clicks
        this.listener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                handleRowClick(v);
            }
        };

        // Search item
        ((TextView)findViewById(R.id.txtID)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){}

            @Override
            public void afterTextChanged(Editable s)
            {
                if(s.length() == Database.BARCODE_SIZE)
                {
                    handleBarcode(s.toString());
                }
            }
        });

        // Spinner actions
        ((Spinner)findViewById(R.id.spinKit)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                int slot = parent.getSelectedItemPosition();

                if(slot > 0)
                {
                    currentKit = kits[slot - 1];
                }
                else
                {
                    currentKit = null;
                    resetForm();
                }

                populateForm();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                currentKit = null;
                populateForm();
            }
        });
    }


    /**
     * Fill from with kit information
     */
    private void populateForm()
    {
        if(currentKit != null)
        {
            Item[] items = this.database.getItemsFromKit(currentKit);

            if(items.length == 0)
                return;

            // Fill form components
            ((TextView)findViewById(R.id.txtName)).setText(currentKit.getName());
            ((TextView)findViewById(R.id.txtDesc)).setText(currentKit.getDescription());

            for(int i = 0; i < items.length; i++)
            {
                addToTable(items[i]);
            }

            findViewById(R.id.txtID).requestFocus();
        }
    }


    /**
     * Handles the event when a barcode is
     * entered.
     * @param id ID of item
     */
    private void handleBarcode(String id)
    {
        NonSellableItem nonSell = database.getNonSellableItem(id); // Look for item
        SellableItem sell = database.getSellableItem(id);

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


        // Reset TextView
        ((TextView)findViewById(R.id.txtID)).setText("");
        findViewById(R.id.txtID).requestFocus();
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

                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
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
        TextView col = (TextView)row.getChildAt(1); // Get quantity column
        int quantity = Integer.parseInt(col.getText().toString());
        col.setText(String.valueOf(quantity + 1)); // Set higher quantity
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

        // Name
        TextView h1 = new TextView(this);
        textViewParams(h1, item.getDescription());
        row.addView(h1);

        // Quantity
        EditText h3 = new EditText(this);
        textViewParams(h3, String.valueOf(item.getQuantity()));
        row.addView(h3);

        // For Sale?
        CheckBox h2 = new CheckBox(this);
        h2.setEnabled(false);
        h2.setGravity(Gravity.CENTER_HORIZONTAL);

        h2.setPadding(15, 12, 15, 12);
        if(item instanceof SellableItem)
            h2.setChecked(true);

        row.addView(h2);

        this.layout.addView(row); // Add to table
        this.list.add(item); // Add to list of items in table
    }


    /**
     * Delete the selected item from the table.
     * @param v Row to delete
     */
    public void deleteItem(View v)
    {
        if(this.viewIndex > -1)
        {
            layout.removeView(layout.getChildAt(viewIndex));
            list.remove(this.viewIndex);
            this.viewIndex = -1;
            colorRows();
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



    private void handleRowClick(View v)
    {
        this.viewIndex = findViewIndex(v); // Get index of clicked row
        findViewById(R.id.btnDelete).setEnabled(true);
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
     * Fill the spinner with kit names
     */
    private void fillSpinner()
    {
        kits = database.getAllKits(); // Get all kits
        Spinner kit_dropdown = findViewById(R.id.spinKit);
        String[] names = new String[this.kits.length + 1];

        names[0] = "New";
        for(int i = 0; i < kits.length; i++)
        {
            names[i + 1] = this.kits[i].getName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names);
        kit_dropdown.setAdapter(adapter);
    }


    /**
     * Sets the header for the table layout.
     */
    private void setHeader()
    {
        TableLayout headerLayout = findViewById(R.id.headerlayout);

        // Set up header row
        TableRow header = new TableRow(this);
        TableRow.LayoutParams p = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        header.setLayoutParams(p);

        // Set header columns
        TextView h1 = new TextView(this);
        textViewParams(h1, "Name");
        header.addView(h1);

        TextView h3 = new TextView(this);
        textViewParams(h3, "Quantity");
        header.addView(h3);

        TextView h4 = new TextView(this);
        textViewParams(h4, "For Sale?");
        header.addView(h4);

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
     * Save the kit to the database.
     * @param view
     */
    public void saveKit(View view)
    {
        if (this.list.size() == 0)
        {
            Toast.makeText(this, "No items to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = ((TextView)findViewById(R.id.txtName)).getText().toString();

        if(name.length() == 0) // Requires name
        {
            Toast.makeText(this, "Kit must have a name", Toast.LENGTH_SHORT).show();
            return;
        }


        String desc = ((TextView)findViewById(R.id.txtDesc)).getText().toString();
        int[] quantity = getQuantity();
        boolean[] sellable = getSellable();
        Item[] items = objectToItem();

        boolean suc = this.database.saveKit(currentKit, name, desc, items, sellable, quantity);

        if(suc)
            Toast.makeText(this, "Save Successful", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Save Not Successful", Toast.LENGTH_SHORT).show();

        fillSpinner();
        resetForm();
    }


    /**
     * Translates object array to item array from list.
     * @return Item array
     */
    private Item[] objectToItem()
    {
        Item[] items = new Item[this.list.size()];

        for(int i = 0; i < items.length; i++)
        {
            items[i] = this.list.get(i);
        }

        return items;
    }


    /**
     * Returns the quantities from the table
     * as an array. Will return null if no rows
     * in table.
     * @return Array with length the same as the table rows or null
     */
    private int[] getQuantity()
    {
        if(this.layout.getChildCount() == 0)
            return null;

        int[] quantity = new int[this.layout.getChildCount()];
        TableRow row;
        EditText text;
        for(int i = 0; i < quantity.length; i++)
        {
            row = (TableRow) this.layout.getChildAt(i);
            text = (EditText) row.getChildAt(1);
            quantity[i] = Integer.parseInt(text.getText().toString());
        }

        return quantity;
    }


    /**
     * Returns the values of checkboxes from the table
     * as an array. Will return null if no rows
     * in table.
     * @return Array with length the same as the table rows or null
     */
    private boolean[] getSellable()
    {
        if(this.layout.getChildCount() == 0)
            return null;

        boolean[] sellable = new boolean[this.layout.getChildCount()];
        TableRow row;
        CheckBox box;
        for(int i = 0; i < sellable.length; i++)
        {
            row = (TableRow) this.layout.getChildAt(i);
            box = (CheckBox) row.getChildAt(2);
            sellable[i] = box.isChecked(); // True/False
        }

        return sellable;
    }


    /**
     * Delete the selected row from table.
     * @param view
     */
    public void deleteRow(View view)
    {
        if(this.viewIndex > -1)
        {
            layout.removeView(layout.getChildAt(viewIndex));
            list.remove(this.viewIndex);
            this.viewIndex = -1;
            colorRows();
            findViewById(R.id.btnDelete).setEnabled(false);
        }
    }


    /**
     * Resets the form to defaults and
     * refreshes thte spinner with kits.
     */
    private void resetForm()
    {
        this.currentKit = null;
        this.layout.removeAllViews(); // Clear table layout
        this.list.clear(); // Clear item list
        findViewById(R.id.txtName).requestFocus();
        ((TextView)findViewById(R.id.txtName)).setText("");
        ((TextView)findViewById(R.id.txtDesc)).setText("");
        ((TextView)findViewById(R.id.txtID)).setText("");
    }


    /**
     * Closes the form.
     */
    public void closeForm(View view)
    {
        this.database.close();
        this.finish();
    }
}
