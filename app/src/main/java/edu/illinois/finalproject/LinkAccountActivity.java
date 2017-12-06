package edu.illinois.finalproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class LinkAccountActivity extends AppCompatActivity {
    private final String tag = "LinkAccountActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_account);
    }

    public void onLinkRequestClicked(View view) {
        Log.d(tag, "Link Request Clicked");
    }
}
