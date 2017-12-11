package edu.illinois.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

//import com.firebase.ui.auth.AuthUI;
//import com.firebase.ui.auth.IdpResponse;
//import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {
    private final String tag = "SettingsActivity";
    private String name, email, linkedEmail;
    private boolean isSignedIn;

    private Button signInButton, linkAccountButton, createAccountButton;
    private TextView nameTextView, emailTextView, linkedTextView, lastTripTextView, durationTextView, alarmsTextView;

    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

//        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        signInButton = (Button) findViewById(R.id.signInButton);
        linkAccountButton = (Button) findViewById(R.id.linkAccountButton);
        nameTextView = (TextView) findViewById(R.id.nameTextView);
        emailTextView = (TextView) findViewById(R.id.emailTextView);
        linkedTextView = (TextView) findViewById(R.id.linkedEmailView);
        lastTripTextView = (TextView) findViewById(R.id.lastTripTextView);
        durationTextView = (TextView) findViewById(R.id.durationTextView);
        alarmsTextView = (TextView) findViewById(R.id.alarmsTextView);

        refreshCurrentUserDisplay();
    }

    public void onSignInOutClicked(View view) {
//        if (currentUser == null) {
//            // No user is currently signed in; try signing the user in
//            List<AuthUI.IdpConfig> providers = Arrays.asList(
//                    new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build());
//
//            startActivityForResult(AuthUI.getInstance()
//                            .createSignInIntentBuilder()
//                            .setAvailableProviders(providers)
//                            .build(),
//                    RC_SIGN_IN);
//        } else {
//            // A user is currently signed in; sign them out
//            AuthUI.getInstance().signOut(this)
//                                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                                    public void onComplete(@NonNull Task<Void> task) {
//                                        currentUser = firebaseAuth.getCurrentUser();
//
//                                        // Refreshes display to current user depending on whether they're signed in
//                                        refreshCurrentUserDisplay();
//                                    }
//                                });
//        }
    }

    public void onLinkAccountClicked(View view) {
        Intent intent = new Intent(this, LinkAccountActivity.class);
        startActivity(intent);
    }

    public void refreshCurrentUserDisplay() {
        if (currentUser == null) {
            nameTextView.setText(R.string.default_sign_in_message);
            emailTextView.setText("");

            signInButton.setText(R.string.sign_in_text);
            linkAccountButton.setVisibility(View.INVISIBLE);
        } else {
            nameTextView.setText(currentUser.getDisplayName());
            emailTextView.setText(currentUser.getEmail());

            final DatabaseReference userDatabaseReference = database.getReference("users/" + currentUser.getUid());
            if (userDatabaseReference.child("child") != null) {

                final DatabaseReference childReference = userDatabaseReference.child("child");
                childReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String childUserID = (String) dataSnapshot.getValue();

                        // Gets the linked user's email address using their User ID
                        DatabaseReference childEmail = database.getReference("users/" + childUserID + "/email");
                        childEmail.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                linkedTextView.setText((String) dataSnapshot.getValue());
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                            }
                        });

                        // Displays the recent trip information of the linked user
                        DatabaseReference childTrips = database.getReference("trips/" + childUserID);
                        if (childTrips != null) {
                            childTrips.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() == null) {
                                        Log.d(tag, "Linked user has not gone on any trips");

                                        // If the linked user has not gone on any trips yet, pass in null for trips
                                        updateLinkedAccountInfo(null);
                                    } else {
                                        for (DataSnapshot childTrip : dataSnapshot.getChildren()) {
                                            updateLinkedAccountInfo(childTrip);
                                            break;
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            } else {
                linkedTextView.setText(R.string.linkedAccountsEmpty);

                // If the user has no linked children, remove the linked user text views
                lastTripTextView.setVisibility(View.INVISIBLE);
                durationTextView.setVisibility(View.INVISIBLE);
                alarmsTextView.setVisibility(View.INVISIBLE);
            }

            signInButton.setText(R.string.sign_out_text);
            linkAccountButton.setVisibility(View.VISIBLE);
        }
    }

    public void updateLinkedAccountInfo(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null) {
            lastTripTextView.setText(R.string.lastTripEmptyText);
            durationTextView.setText("");
            alarmsTextView.setText("");
        } else {
            // Gets the start time of the most recent trip and displays to user
            String lastTripText = "Last Trip: " + dataSnapshot.getKey();
            lastTripTextView.setText(lastTripText);

            // Gets the duration of the most recent trip and displays to user
            if (dataSnapshot.hasChild("start_time") && dataSnapshot.hasChild("end_time")) {
                Long startTime = (Long) dataSnapshot.child("start_time").getValue();
                Long endTime = (Long) dataSnapshot.child("end_time").getValue();

                String minutes = Long.toString(TimeUnit.MILLISECONDS.toMinutes(endTime - startTime));

                String durationText = "Duration: " + minutes + " minutes";
                durationTextView.setText(durationText);
            } else {
                durationTextView.setText(R.string.durationEmptyText);
            }

            // Gets the number of alarms set off during the most recent trip and displays to user
            if (dataSnapshot.hasChild("alarms")) {
                long alarms = dataSnapshot.getChildrenCount();
                String alarmsText = "Alarms: " + Long.toString(alarms);
                alarmsTextView.setText(alarmsText);
            } else {
                alarmsTextView.setText(R.string.alarmsEmptyText);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // WRITE USER TO DATABASE WITH EMAIL ADDRESS
//        if (requestCode == RC_SIGN_IN) {
//            IdpResponse response = IdpResponse.fromResultIntent(data);
//
//            if (resultCode == RESULT_OK) {
//                // Successfully signed in
//                currentUser = FirebaseAuth.getInstance().getCurrentUser();
//
//                refreshCurrentUserDisplay();
//            } else {
//                // Sign in failed, check response for error code
//            }
//        }
    }
}
