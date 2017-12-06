package edu.illinois.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class SettingsActivity extends AppCompatActivity {
    private String name, email, linkedEmail;
    private boolean isSignedIn;

    private Button signInButton, linkAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        signInButton = (Button)findViewById(R.id.signInButton);
        linkAccountButton = (Button)findViewById(R.id.linkAccountButton);
    }

    public void onSignInClicked(View view) {
        
    }
    public void onLinkAccountClicked(View view) {
        Intent intent = new Intent(this, LinkAccountActivity.class);
        startActivity(intent);
    }
}
