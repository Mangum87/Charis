package com.charis;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.charis.data.User;

public class MainActivity extends AppCompatActivity
{
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.user = (User)getIntent().getSerializableExtra("user");
    }


    /**
     * Perform logout procedures and
     * launch login activity.
     * @param view
     */
    public void logout(View view)
    {
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


    /**
     * Open distribution screen for selling/giving away items.
     * Pass user object to activity.
     * @param view
     */
    public void openDistribution(View view)
    {
        Intent intent = new Intent(this, Distribution.class);
        intent.putExtra("user", user); // Pass user object
        startActivity(intent);
    }
    
    /**
     * open new item screen for creating and editing items
     */
    public void openNewItem(View view)
    {
        Intent intent = new Intent(this, NewItem.class);
        startActivity(intent);
    }


    public void openKits(View view)
    {
        Intent intent = new Intent(this, KitActivity.class);
        startActivity(intent);
    }

    public void openUsers(View view)
    {
        Intent intent = new Intent(this, user.class);
        startActivity(intent);
    }
}
