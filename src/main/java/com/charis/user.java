package com.charis;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        //open the database
        this.database = new Database();

        // Linking buttons
        name = (EditText) findViewById(R.id.user_name);
        firstname = (EditText) findViewById(R.id.first_name);
        lastname = (EditText) findViewById(R.id.last_name);
        password = (EditText) findViewById(R.id.editText);
        confirmpw = (EditText) findViewById(R.id.editText2);
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

        User user = this.database.getUser(username); // Query database for user

        if(user != null) //update user if exist
        {
            update = this.database.updatePassword(user); //updates to new password
            if(update)
            {
                User updateAcc = new User(username, first_name, last_name, pass, admin, active);
                success = database.updateUser(updateAcc);

                if (success)
                {
                    Toast.makeText(this, "Update Success", Toast.LENGTH_SHORT).show();
                    clear_form(view);
                }
                else
                {
                    Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else //create new user
        {
            this.database.createUser(username, pass, admin, active, first_name, last_name);
            success_toast.show();
            clear_form(view);
        }
    }



    /**
     *  clear text in form
     */
    public void clear_form (View view){
        EditText name = findViewById(R.id.user_name);
        EditText firstname = findViewById(R.id.first_name);
        EditText lastname = findViewById(R.id.last_name);
        EditText password = findViewById(R.id.editText);
        EditText confirmpw = findViewById(R.id.editText2);
        RadioButton radio_admin = findViewById(R.id.radio_admin2);
        RadioButton radio_active = findViewById(R.id.radio_active2);

        name.getText().clear();
        firstname.getText().clear();
        lastname.getText().clear();
        password.getText().clear();
        confirmpw.getText().clear();

        radio_admin.setEnabled(true);
        radio_active.setEnabled(true);
    }

}
