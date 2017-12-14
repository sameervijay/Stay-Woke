package edu.illinois.finalproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    private Button startTripButton;
    private String logTag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startTripButton = (Button)findViewById(R.id.startTripButton);
    }

    /**
     * Called when "Start Trip" button is clicked (the button with the car icon); initiates an intent with
     * Trip Activity
     * @param view button that was clicked
     */
    public void onStartTripClicked(View view) {
        Intent intent = new Intent(this, TripActivity.class);
        startActivity(intent);

        Log.d(logTag, "Start trip was pressed");
    }

    // Toolbar code derived from: https://stackoverflow.com/questions/31231609/creating-a-button-in-android-toolbar
    @Override
    /**
     * Inflates the menu bar that contains the settings button
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    /**
     * Called when the settings button in the toolbar is tapped. Creates an intent with the SettingsActivity
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return super.onOptionsItemSelected(item);
    }
}
