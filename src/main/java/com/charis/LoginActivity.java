
package com.charis;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.charis.util.Database;
import com.charis.data.User;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener
{
    private EditText name , password;
    private Button submit;
    private Database access;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Linking buttons
        name = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        submit =  (Button) findViewById(R.id.submit);

        submit.setOnClickListener(this);

        this.access = new Database(); // Open database
    }


    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.submit:
                String username = name.getText().toString(); // Username input
                String pass = password.getText().toString(); // Password input

                User user = this.access.getUser(username); // Query database for user

                // Check for valid user object
                if(user != null)
                {
                    if(access.checkHashedPassword(pass, user.getPassword())) // Check password match
                    {
                        if(user.isActive()) // Is user allowed to log in?
                        {
                            this.access.close(); // Close database

                            // Create intent to open MainActivity
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra("user", user); // Pass user object
                            startActivity(intent);
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "Log in access denied", Toast.LENGTH_LONG).show();
                            resetComponents(); // Reset window components
                        }
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                        this.password.setText("");
                        password.setBackgroundColor(Color.RED);
                    }
                }
                else
                {
                    Toast.makeText(this, username + " not found", Toast.LENGTH_SHORT).show();
                    resetComponents();
                }

                break;
        }
    }


    /**
     * Reset the GUI components to base values.
     */
    private void resetComponents()
    {
        this.name.setText("");
        this.password.setText("");
        this.name.requestFocus();
    }


    /**
     * Returns database connection object.
     * @return DatabaseAccess object
     */
    public Database getDatabase()
    {
        return this.access;
    }
}