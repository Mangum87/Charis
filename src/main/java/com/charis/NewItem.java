package com.charis;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.charis.data.Category;
import com.charis.data.Enum.Condition;
import com.charis.data.Item;
import com.charis.data.Location;
import com.charis.data.NonSellableItem;
import com.charis.data.SellableItem;
import com.charis.util.Database;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class NewItem extends AppCompatActivity
{
    int sellable = 0; //false
    boolean update = false; // Default to new item
    int new_cat = 0; //false
    int new_loc = 0; //false

    private Database database;
    private static final String TAG = "NewItem";
    private DatePickerDialog.OnDateSetListener DateSetListener;
    private AdapterView.OnItemSelectedListener condSpinListener;
    private AdapterView.OnItemSelectedListener catSpinListener;
    private AdapterView.OnItemSelectedListener locSpinListener;
    private Date pickedDate;

    private Location[] locations; // List of all location in database
    private Location currentLoc; // Current selected location in spinner
    private Condition condition; // Current selected condition in spinner
    private Category[] categories; // List of all categories in database
    private Category currentCat; // Current selected category in spinner

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newitem);

        //open the database
        this.database = new Database();

        //create condition spinner and set values in it
        Spinner condition_dropdown = findViewById(R.id.spinner1);
        String[] conditions = new String[]{"Poor", "Good", "Excellent"};
        ArrayAdapter<String> condition_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, conditions);
        condition_dropdown.setAdapter(condition_adapter);

        //category spinner
        Spinner category_dropdown = findViewById(R.id.spinner2);
        String[] categories_spinner = getCategoryStrings();
        ArrayAdapter<String> category_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories_spinner);
        category_dropdown.setAdapter(category_adapter);

        // Location spinner
        Spinner locDropdown = findViewById(R.id.locSpinner);
        String[] locSpinner = getLocationStrings();
        ArrayAdapter<String> location_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, locSpinner);
        locDropdown.setAdapter(location_adapter);

        //create the calendar
        final TextView DisplayDate = findViewById(R.id.Select_Date);
        DisplayDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dialog = new DatePickerDialog(NewItem.this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, DateSetListener, year,month,day);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        DateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.MONTH, month - 1); // Zero based tsart
                cal.set(Calendar.DAY_OF_MONTH, day);
                cal.set(Calendar.YEAR, year);
                pickedDate = cal.getTime();

                Log.d(TAG, "onDateSet: mm/dd/yyy: " + month + "/" + day + "/" + year);

                String date = month + "/" + day + "/" + year;
                DisplayDate.setText(date);
            }
        };

        // Listeners for spinners
        this.condSpinListener = new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                condition = Condition.toCondition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        };

        this.catSpinListener = new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (parent.getItemAtPosition(position).equals("New")) {
                    new_category(view);
                    new_cat = 1;
                } else {
                    new_cat = 0;
                    currentCat = categories[position];
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        };

        this.locSpinListener = new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (parent.getItemAtPosition(position).equals("New")) {
                    new_location(view);
                    new_loc = 1;
                } else {
                    new_loc = 0;
                    currentLoc = locations[position];
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        };


        condition_dropdown.setOnItemSelectedListener(this.condSpinListener);
        category_dropdown.setOnItemSelectedListener(this.catSpinListener);
        ((Spinner)findViewById(R.id.locSpinner)).setOnItemSelectedListener(this.locSpinListener);

        TextView category_view = findViewById(R.id.textView14);
        category_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (new_cat == 0) {
                    edit_category(view);
                } else {
                    Toast.makeText(getApplicationContext(), "Category Cannot be Edited", Toast.LENGTH_SHORT).show();
                }
            }
        });
        TextView location_view = findViewById(R.id.textView16);
        location_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (new_loc == 0) {
                    edit_location(view);
                } else {
                    Toast.makeText(getApplicationContext(), "Location Cannot be Edited", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void find_item(View view)
    {
        EditText barcode = findViewById(R.id.edit_barcode);

        SellableItem sell_item_found;
        NonSellableItem non_sell_item_found;

        non_sell_item_found = database.getNonSellableItem(barcode.getText().toString());
        sell_item_found = database.getSellableItem(barcode.getText().toString());

        if (non_sell_item_found != null && sell_item_found == null)
        {
            findViewById(R.id.edit_barcode).setEnabled(false);
            this.update = true;
            NonSellable_fill_blanks(non_sell_item_found);
        } else if(sell_item_found != null && non_sell_item_found == null)
        {
            findViewById(R.id.edit_barcode).setEnabled(false);
            this.update = true;
            fill_blanks(sell_item_found);
            sellable = 1; //true
        }
        else if(sell_item_found != null && non_sell_item_found != null)
        {
            findViewById(R.id.edit_barcode).setEnabled(false);
            this.update = true;
            // Give alert dialog to allow user to decide which to fill
            showDialog(sell_item_found, non_sell_item_found);
        }
        else
        {
            this.update = false;
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_SHORT;
            Toast message = Toast.makeText(context, "Item Does Not Exist", duration);
            message.show();
        }
    }


    /**
     * Show user options to fill form with information
     * from database.
     * @param i1 SellableItem object
     * @param i2 NonSellableItem object
     */
    private void showDialog(final SellableItem i1, final NonSellableItem i2)
    {
        final String[] list = {"Sold items", "Free items"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Display which item");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setItems(list, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch(which)
                {
                    case 0:
                        sellable = 1;
                        fill_blanks(i1);
                        break;
                    case 1:
                        sellable = 0;
                        NonSellable_fill_blanks(i2);
                        break;
                }

                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }



    /** fill form with Database values */
    private void NonSellable_fill_blanks(NonSellableItem ITEM)
    {
        fillCommonElements(ITEM);

        EditText source = findViewById(R.id.edit_source);
        source.setText(ITEM.getSource());

        //set radio button
        RadioButton Non_Sell = findViewById(R.id.radio_nonsellable);
        Non_Sell.setChecked(true);
    }

    /* fill form with Database values */
    private void fill_blanks(SellableItem ITEM)
    {
        fillCommonElements(ITEM);

        //set radio button
        RadioButton Radio_Sell = findViewById(R.id.radio_sellable);
        Radio_Sell.setChecked(true);

        findViewById(R.id.edit_source).setEnabled(false);
    }


    /**
     * Fill form with common elements between
     * sellable and nonsellable objects.
     * @param ITEM Item to fill form from
     */
    private void fillCommonElements(Item ITEM)
    {
        RadioButton Non_Sell = findViewById(R.id.radio_nonsellable);
        RadioButton Radio_Sell = findViewById(R.id.radio_sellable);
        EditText price = findViewById(R.id.edit_price);
        EditText quantity = findViewById(R.id.edit_quantity);
        EditText description = findViewById(R.id.edit_description);
        Spinner category = findViewById(R.id.spinner2);
        Spinner location = findViewById(R.id.locSpinner);

        //set all editText fields to DB values
        String item_price = String.valueOf(ITEM.getPrice());
        price.setText(item_price);
        CharSequence item_quantity = String.valueOf(ITEM.getQuantity());
        quantity.setText(item_quantity);
        description.setText(ITEM.getDescription());

        // Set calendar values
        this.pickedDate = ITEM.getReceived();
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.pickedDate);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);
        String text = month + "/" + day + "/" + year;
        ((TextView)findViewById(R.id.Select_Date)).setText(text);

        //set the condition on the spinner
        Spinner condition = findViewById(R.id.spinner1);
        Condition item_condition = ITEM.getCondition();
        condition.setSelection(Condition.toInt(item_condition));

        // Set selection of category
        int index = getCategoryIndex(ITEM.getCategory());
        category.setSelection(index);

        // Set selection of location
        index = getLocationIndex(ITEM.getLocation());
        location.setSelection(index);

        Non_Sell.setEnabled(false);
        Radio_Sell.setEnabled(false);
    }


    /**
     * Returns the index of the category for
     * the spinner. Default return if category
     * is not found is -1.
     * @param c Category to search for
     * @return Index of category or -1 if not found
     */
    private int getCategoryIndex(Category c)
    {
        for(int i = 0; i < this.categories.length; i++)
        {
            if(c.getID().equals(this.categories[i].getID()))
                return i;
        }

        return -1;
    }


    /**
     * Returns the index of the location for
     * the spinner. Default return if location
     * is not found is -1.
     * @param l Location to search for
     * @return Index of location or -1 if not found
     */
    private int getLocationIndex(Location l)
    {
        for(int i = 0; i < this.locations.length; i++)
        {
            if(l.getID().equals(this.locations[i].getID()))
                return i;
        }

        return -1;
    }

    public void save_data(View view) {
        EditText description = findViewById(R.id.edit_description);

        EditText price = findViewById(R.id.edit_price);
        double item_price = Double.parseDouble(price.getText().toString().substring(1));

        EditText quantity = findViewById(R.id.edit_quantity);
        int item_quantity = Integer.parseInt(quantity.getText().toString());

        EditText source = findViewById(R.id.edit_source);

        boolean success;
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast success_toast = Toast.makeText(context, "Item Created Successfully", duration);
        Toast failure_toast = Toast.makeText(context, "Failed to Create Item", duration);

        if(this.update)
        {

            String id = ((TextView)findViewById(R.id.edit_barcode)).getText().toString();

            if(sellable == 1) // Update sellable
            {
                SellableItem item = new SellableItem(id, pickedDate, description.getText().toString(), item_quantity, condition, item_price, currentCat, currentLoc);
                success = database.updateSellable(item);

                if(success)
                {
                    findViewById(R.id.edit_barcode).setEnabled(true); // Allow changes
                    Toast.makeText(this, "Update Succeeded", Toast.LENGTH_SHORT).show();
                    clear_form(view);
                }
                else
                {
                    Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
                }
            }
            else // Update nonsellable
            {
                NonSellableItem item = new NonSellableItem(id, pickedDate, description.getText().toString(), item_quantity, condition, item_price, currentCat, source.getText().toString(), currentLoc);
                success = database.updateNonSellable(item);

                if(success)
                {
                    findViewById(R.id.edit_barcode).setEnabled(true); // Allow changes
                    Toast.makeText(this, "Update Succeeded", Toast.LENGTH_SHORT).show();
                    clear_form(view);
                }
                else
                {
                    Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else // Make new item
        {
                if (sellable == 1) {//sellable
                    success = database.createSellableItem(pickedDate, description.getText().toString(), condition, item_price, currentCat, item_quantity, currentLoc);
                    if (success) {
                        findViewById(R.id.edit_barcode).setEnabled(true); // Allow changes
                        success_toast.show();
                        clear_form(view);
                    } else {
                        failure_toast.show();
                    }
                } else {
                    success = database.createNonSellableItem(pickedDate, description.getText().toString(), condition, item_price, currentCat, source.getText().toString(), item_quantity, currentLoc);
                    if (success) {
                        findViewById(R.id.edit_barcode).setEnabled(true); // Allow changes
                        success_toast.show();
                        clear_form(view);
                    } else {
                        failure_toast.show();
                    }
                }
        }
    }


    /**
     * Close the activity
     */
    public void return_prev (View view)
    {
        this.database.close();
        this.finish();
    }

    public void onRadioButtonClicked (View view){
        boolean check_clicked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.radio_nonsellable:
                if (check_clicked) {
                    sellable = 0;//false
                    break;
                }
            case R.id.radio_sellable:
                if (check_clicked) {
                    sellable = 1; //true
                    break;
                }
        }
    }

    /**
     *  clear text in form
     */
    public void clear_form (View view){
        EditText Bar = findViewById(R.id.edit_barcode);
        EditText Des = findViewById(R.id.edit_description);
        EditText quan = findViewById(R.id.edit_quantity);
        EditText price = findViewById(R.id.edit_price);
        EditText source = findViewById(R.id.edit_source);
        RadioButton Non_Sell = findViewById(R.id.radio_nonsellable);
        RadioButton Radio_Sell = findViewById(R.id.radio_sellable);

        Bar.getText().clear();
        Des.getText().clear();
        quan.getText().clear();
        price.getText().clear();
        source.getText().clear();


        this.pickedDate = null;
        ((TextView)findViewById(R.id.Select_Date)).setText("");
        findViewById(R.id.edit_barcode).setEnabled(true); // Allow changes

        ((Spinner)findViewById(R.id.spinner1)).setSelection(0);
        ((Spinner)findViewById(R.id.spinner2)).setSelection(0);
        ((Spinner)findViewById(R.id.locSpinner)).setSelection(0);


        Non_Sell.setEnabled(true);
        Radio_Sell.setEnabled(true);
        source.setEnabled(true);

    }


    /**
     * Returns an array of all category
     * names.
     * @return Array of category names
     */
    private String[] getCategoryStrings()
    {
        this.categories = this.database.getAllCategories();
        int length = categories.length;
        String[] names = new String[length + 1];
        names[length] = "New";

        for(int i = 0; i < length; i++)
        {
            names[i] = categories[i].getName();
        }

        return names;
    }

    /**
     * Returns an array of all location names.
     *
     */
    private String[] getLocationStrings() {
        this.locations = this.database.getAllLocations();
        int length = locations.length;
        String[] names = new String[length+1];
        names[length] = "New";

        for (int i = 0; i < length; i++) {
            names[i] = locations[i].getName();
        }

        return names;
    }

    /**
     *  create new category
     */
    private String category_Text = "";
    private void new_category(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create new Category");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //update spinner
                category_Text = input.getText().toString();
                database.createCategory(category_Text);

                Spinner category_dropdown = findViewById(R.id.spinner2);
                String[] categories_spinner = getCategoryStrings();
                ArrayAdapter<String> category_adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, categories_spinner);
                category_dropdown.setAdapter(category_adapter);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * edit category
     *
     */
    public void edit_category(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Category " + currentCat.getName());

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String edit_cat_text = input.getText().toString();
                currentCat.setName(edit_cat_text);
                database.updateCategory(currentCat);

                Spinner category_dropdown = findViewById(R.id.spinner2);
                String[] categories_spinner = getCategoryStrings();
                ArrayAdapter<String> category_adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, categories_spinner);
                category_dropdown.setAdapter(category_adapter);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /**
     *  create new location
     */
    private String location_Text = "";
    private void new_location(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Location");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //update spinner
                location_Text = input.getText().toString();
                database.createLocation(location_Text);

                Spinner location_dropdown = findViewById(R.id.locSpinner);
                String[] location_spinner = getLocationStrings();
                ArrayAdapter<String> location_adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, location_spinner);
                location_dropdown.setAdapter(location_adapter);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void edit_location(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Location " + currentLoc.getName());

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String edit_loc_text = input.getText().toString();
                currentLoc.setName(edit_loc_text);
                database.updateLocation(currentLoc);

                Spinner location_dropdown = findViewById(R.id.locSpinner);
                String[] location_spinner = getLocationStrings();
                ArrayAdapter<String> location_adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, location_spinner);
                location_dropdown.setAdapter(location_adapter);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}
