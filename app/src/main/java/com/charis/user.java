package com.charis;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.charis.util.Database;
import com.charis.data.User;

public class user extends AppCompatActivity {

    private EditText name , password, firstname, lastname, confirmpw;
    private Database database;
    boolean admin = false;
    boolean active = false;
    boolean update = false;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        //open the database
        this.database = new Database();
        this.user = null;

        // Linking buttons
        name = (EditText) findViewById(R.id.user_name);
        name.requestFocus();
        firstname = (EditText) findViewById(R.id.first_name);
        lastname = (EditText) findViewById(R.id.last_name);
        password = (EditText) findViewById(R.id.editText);
        confirmpw = (EditText) findViewById(R.id.editText2);


        name.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if(s.length() > 0)
                {
                    User u = database.getUser(s.toString()); // Look for user

                    if (u != null)
                    {
                        user = u;
                        fillForm(user);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Close the activity
     */
    public void return_prev (View view)
    {
        this.database.close();
        this.finish();
    }


    /**
     * Run the click event for radio buttons.
     * @param view
     */
    public void onRadioButtonClicked (View view){
        boolean check_clicked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.radio_admin1:
                if (check_clicked) {
                    admin = true;//true
                    break;
                }
            case R.id.radio_admin2:
                if (check_clicked) {
                    admin = false; //false
                    break;
                }
            case R.id.radio_active1:
                if (check_clicked) {
                    active = true;//true
                    break;
                }
            case R.id.radio_active2:
                if (check_clicked) {
                    active = false; //false
                    break;
                }
        }
    }


    /**
     * Save the form data to the database.
     * @param view
     */
    public void save_data(View view)
    {
        String username = name.getText().toString(); // Username input
        String pass = password.getText().toString(); // Password input
        String pass1 = confirmpw.getText().toString();
        String first_name = firstname.getText().toString();
        String last_name = lastname.getText().toString();

        boolean success;
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast success_toast = Toast.makeText(context, "User Created Successfully", duration);

        if(user != null) //update user if exist
        {
            if(pass.length() > 0) // Check if password should update
            {
                if(pass.equals(pass1))
                {
                    user.setPassword(pass); // Set new password
                    update = this.database.updatePassword(user); //updates to new password
                }
                else
                {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_LONG).show();
                    confirmpw.requestFocus();
                    return;
                }
            }
            else
                update = true; // Assume no password update


            // Update user data
            user.setFirstName(first_name);
            user.setLastName(last_name);
            user.setAdmin(admin);
            user.setActive(active);
            success = database.updateUser(user);


            if (update && success)
            {
                Toast.makeText(this, "Update Success", Toast.LENGTH_SHORT).show();
                clear_form();
            }
            else
            {
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        }
        else //create new user
        {
            this.database.createUser(username, pass, admin, active, first_name, last_name);
            success_toast.show();
            clear_form();
        }
    }


    /**
     * Execute button push to clear form.
     * @param view
     */
    public void clear(View view)
    {
        clear_form();
    }



    /**
     *  clear text in form
     */
    private void clear_form()
    {
        RadioButton radio_admin = findViewById(R.id.radio_admin2);
        RadioButton radio_active = findViewById(R.id.radio_active2);

        name.getText().clear();
        name.requestFocus();
        firstname.getText().clear();
        lastname.getText().clear();
        password.getText().clear();
        confirmpw.getText().clear();

        radio_admin.setEnabled(true);
        radio_active.setEnabled(true);
        name.setEnabled(true);
    }


    /**
     * Fill the form with user data.
     * @param u User to fill data with
     */
    private void fillForm(User u)
    {
        name.setEnabled(false);
        ((EditText)findViewById(R.id.first_name)).setText(u.getFirstName());
        ((EditText)findViewById(R.id.last_name)).setText(u.getLastName());

        if(u.isAdmin())
            ((RadioButton)findViewById(R.id.radio_admin1)).setChecked(true);
        else
            ((RadioButton)findViewById(R.id.radio_admin2)).setChecked(true);

        if(u.isActive())
            ((RadioButton)findViewById(R.id.radio_active1)).setChecked(true);
        else
            ((RadioButton)findViewById(R.id.radio_active2)).setChecked(true);

        admin = u.isAdmin();
        active = u.isActive();
    }
}
