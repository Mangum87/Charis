package com.charis;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
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
        setComponents();
    }


    /**
     * Set component interaction based
     * on privilege level.
     */
    private void setComponents()
    {
        if(!user.isAdmin())
        {
            findViewById(R.id.imageButton).setEnabled(false);
            findViewById(R.id.imageButton3).setEnabled(false);
            findViewById(R.id.imageButton6).setEnabled(false);
            findViewById(R.id.imageButton5).setEnabled(false);


            findViewById(R.id.imageButton).setVisibility(View.INVISIBLE);
            findViewById(R.id.textView10).setVisibility(View.INVISIBLE);
            findViewById(R.id.imageButton3).setVisibility(View.INVISIBLE);
            findViewById(R.id.textView11).setVisibility(View.INVISIBLE);
            findViewById(R.id.imageButton6).setVisibility(View.INVISIBLE);
            findViewById(R.id.textView15).setVisibility(View.INVISIBLE);
            findViewById(R.id.imageButton5).setVisibility(View.INVISIBLE);
            findViewById(R.id.textView13).setVisibility(View.INVISIBLE);
        }
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
     * Opens thte report activity.
     * @param view
     */
    public void openReport(View view)
    {
        Intent intent = new Intent(this, ReportActivity.class);
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


    /**
     * Opens the kit activity.
     * @param view
     */
    public void openKits(View view)
    {
        Intent intent = new Intent(this, KitActivity.class);
        startActivity(intent);
    }


    /**
     * Opens the user activity
     * @param view
     */
    public void openUsers(View view)
    {
        Intent intent = new Intent(this, user.class);
        startActivity(intent);
    }


    /**
     * Open the barcode printing activity
     * @param view
     */
    public void openPrint(View view)
    {

        Intent intent = new Intent(this, PrintActivity.class);
        startActivity(intent);
    }
}
