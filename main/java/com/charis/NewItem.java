package com.charis;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.charis.data.Category;
import com.charis.data.Enum.Condition;
import com.charis.data.Location;
import com.charis.data.NonSellableItem;
import com.charis.data.SellableItem;
import com.charis.util.Database;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class NewItem extends AppCompatActivity
{
    int new_item = 0; //false
    int sellable = 0; //false

    private Database database;
    private static final String TAG = "NewItem";
    private DatePickerDialog.OnDateSetListener DateSetListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newitem);

        //create condition spinner and set values in it
        Spinner condition_dropdown = findViewById(R.id.spinner1);
        String[] conditions = new String[]{"Excellent", "Good", "Poor"};
        ArrayAdapter<String> condition_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, conditions);
        condition_dropdown.setAdapter(condition_adapter);

        //category spinner
        Spinner category_dropdown = findViewById(R.id.spinner2);
        String[] categories_spinner = new String[]{"category1", "category2"};
        //TODO - get all categories and print in spinner - uncomment code in fill blanks functions when this is completed
        ArrayAdapter<String> category_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories_spinner);
        category_dropdown.setAdapter(category_adapter);

        //open the database
        this.database = new Database();

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
                Log.d(TAG, "onDateSet: mm/dd/yyy: " + month + "/" + day + "/" + year);

                String date = month + "/" + day + "/" + year;
                DisplayDate.setText(date);
            }
        };
    }


    /*  send message to user that a new item will be created when the save button is clicked
     *  set newitem int to 1 to indicate true
     */
    public void create_new(View view) {
        String string = "Barcode generated at Save";
        ((TextView)findViewById(R.id.edit_barcode)).setText(string);
        new_item = 1;
    }

    public void find_item(View view) {

        EditText barcode = findViewById(R.id.edit_barcode);

        SellableItem sell_item_found;
        NonSellableItem non_sell_item_found;

        non_sell_item_found = database.getNonSellableItem(barcode.getText().toString());
        sell_item_found = database.getSellableItem(barcode.getText().toString());

        if (non_sell_item_found != null) {
            NonSellable_fill_blanks(non_sell_item_found);
        } else if(sell_item_found != null) {
            fill_blanks(sell_item_found);
            sellable = 1; //true
        } else {
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_SHORT;
            Toast message = Toast.makeText(context, "Item Does Not Exist", duration);
            message.show();
        }
    }
    /* fill form with Database values */
    private void NonSellable_fill_blanks(NonSellableItem ITEM) {
        EditText source = findViewById(R.id.edit_source);
        EditText price = findViewById(R.id.edit_price);
        EditText quantity = findViewById(R.id.edit_quantity);
        EditText description = findViewById(R.id.edit_description);

        //set all editText fields to DB values
        source.setText(ITEM.getLocation().getName());
        CharSequence item_price = String.valueOf((int)ITEM.getPrice());
        price.setText(item_price);
        CharSequence item_quantity = String.valueOf((int)ITEM.getPrice());
        quantity.setText(item_quantity);
        description.setText(ITEM.getDescription());

        //TODO - Set calendar values
        //set the condition on the spinner
        Spinner condition = findViewById(R.id.spinner1);
        Condition item_condition = ITEM.getCondition();
        condition.setSelection(Condition.toInt(item_condition));

        //set the category on the spinner
        //Spinner category = findViewById(R.id.spinner2);
        //TODO - Set category of item from spinner
        //set radio button
        RadioButton Non_Sell = findViewById(R.id.radio_nonsellable);
        Non_Sell.setChecked(true);
    }

    /* fill form with Database values */
    private void fill_blanks(SellableItem ITEM) {
        EditText source = findViewById(R.id.edit_source);
        EditText price = findViewById(R.id.edit_price);
        EditText quantity = findViewById(R.id.edit_quantity);
        EditText description = findViewById(R.id.edit_description);

        //set all editText fields to DB values
        source.setText(ITEM.getLocation().getName());
        CharSequence item_price = String.valueOf((int)ITEM.getPrice());
        price.setText(item_price);
        CharSequence item_quantity = String.valueOf((int)ITEM.getPrice());
        quantity.setText(item_quantity);
        description.setText(ITEM.getDescription());

        //TODO set calendar values

        //set the condition on the spinner
        Spinner condition = findViewById(R.id.spinner1);
        Condition item_condition = ITEM.getCondition();
        condition.setSelection(Condition.toInt(item_condition));

        //set the category on the spinner
        //Spinner category = findViewById(R.id.spinner2);
        //TODO - change category in item from spinner option

        //set radio button
        RadioButton Radio_Sell = findViewById(R.id.radio_sellable);
        Radio_Sell.setChecked(true);
    }
    public static java.util.Date getDateFromDatePicker (DatePicker datePicker){
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        return calendar.getTime();
    }
    public void save_data(View view, DatePicker datepicker) {
        EditText description = findViewById(R.id.edit_description);

        EditText price = findViewById(R.id.edit_price);
        double item_price = Double.parseDouble(price.getText().toString());

        EditText quantity = findViewById(R.id.edit_quantity);
        int item_quantity = Integer.parseInt(quantity.getText().toString());

        EditText location = findViewById(R.id.edit_source);
        Location item_source = null;
        item_source.setName(location.getText().toString());

        //get date from datepicker calendar
        Date recieved = null;
        //getDateFromDatePicker(datepicker);

        Spinner condition = findViewById(R.id.spinner1);
        Condition item_condition;
        item_condition = Condition.toCondition(condition.getSelectedItemPosition());

        //Spinner category = findViewById(R.id.spinner2);
        Category item_category = null;
        //TODO - category from spinner

        boolean success;
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast success_toast = Toast.makeText(context, "Item Created Successfully", duration);
        Toast failure_toast = Toast.makeText(context, "Failed to Create Item", duration);

        if (new_item == 1) { //create new item
            if (sellable == 1) {//sellable
                success = database.createSellableItem(recieved, description.getText().toString(), item_condition, item_price, item_category, item_quantity, item_source);
                if (success) {
                    success_toast.show();
                    clear_form(view);
                } else {
                    failure_toast.show();
                }
            } else {
                success = database.createNonSellableItem(recieved, description.getText().toString(), item_condition, item_price, item_category, location.getText().toString(), item_quantity, item_source);
                if (success) {
                    success_toast.show();
                    clear_form(view);
                } else {
                    failure_toast.show();
                }
            }
        } else {
            Toast not_new = Toast.makeText(context, "This item already exists", duration);
            not_new.show();
        }
    }


        /**
         * Close the activity
         */
        public void return_prev (View view)
        {
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
        /*  clear text in form */
        public void clear_form (View view){
            EditText Bar = findViewById(R.id.edit_barcode);
            EditText Des = findViewById(R.id.edit_description);
            EditText quan = findViewById(R.id.edit_quantity);
            EditText price = findViewById(R.id.edit_price);
            EditText source = findViewById(R.id.edit_source);

            Bar.getText().clear();
            Des.getText().clear();
            quan.getText().clear();
            price.getText().clear();
            source.getText().clear();

            //reset calendar
            Calendar c = Calendar.getInstance();
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog =
                    new DatePickerDialog(this, DateSetListener, mYear, mMonth, mDay);
            dialog.show();

        }
    }
