package com.charis;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;

import com.charis.data.User;
import com.charis.util.Database;

public class MainActivity extends AppCompatActivity
{
    Database db;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.db = new Database();
        this.user = (User)getIntent().getSerializableExtra("user");
    }


    /**
     * Perform logout procedures and
     * launch login activity.
     * @param view
     */
    public void logout(View view)
    {
        this.db.close(); // Close database

        // Create intent to open LoginActivity
        Intent intent = new Intent(this, LoginActivity.class); // Set up activity launch
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear call stack
        startActivity(intent); // Launch activity
    }


    /**
     * Opens thte inventory activity
     * @param view
     */
    public void openInventory(View view)
    {
        Intent intent = new Intent(this, Inventory.class);
        startActivity(intent);
    }
}
