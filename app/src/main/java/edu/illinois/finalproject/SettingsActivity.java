package edu.illinois.finalproject;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.ParseException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

//import com.firebase.ui.auth.AuthUI;
//import com.firebase.ui.auth.IdpResponse;
//import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {
    private final String tag = "SettingsActivity";
    private static final int MILLISECONDS_IN_ONE_SECOND = 1000;
    private static final int SECONDS_IN_ONE_MINUTE = 60;

    private Button signInButton, linkAccountButton;
    private TextView nameTextView, emailTextView;
    private TextView linkedToTextView, linkedTextView, lastTripTextView, durationTextView, alarmsTextView;

    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();

        signInButton = (Button) findViewById(R.id.signInButton);
        linkAccountButton = (Button) findViewById(R.id.linkAccountButton);
        nameTextView = (TextView) findViewById(R.id.nameTextView);
        emailTextView = (TextView) findViewById(R.id.emailTextView);

        linkedToTextView = (TextView) findViewById(R.id.linkedToTextView);
        linkedTextView = (TextView) findViewById(R.id.linkedEmailView);
        lastTripTextView = (TextView) findViewById(R.id.lastTripTextView);
        durationTextView = (TextView) findViewById(R.id.durationTextView);
        alarmsTextView = (TextView) findViewById(R.id.alarmsTextView);

        refreshCurrentUserDisplay();
    }

    /**
     * Called when the user taps the "Sign In/Out" button (the button is reused for both signing in and out, and the
     * text on the button changes depending on whether currentUser is null
     * @param view "Sign In/Out" button that was tapped
     */
    public void onSignInOutClicked(View view) {
        if (currentUser == null) {
            // No user is currently signed in; try signing the user in
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build());

            startActivityForResult(AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        } else {
            // A user is currently signed in; sign them out
            AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        public void onComplete(@NonNull Task<Void> task) {
                            currentUser = firebaseAuth.getCurrentUser();

                            // Refreshes display to current user depending on whether they're signed in
                            refreshCurrentUserDisplay();
                        }
                    });
        }
    }

    /**
     * Called when the user taps the "Link Account" button. Starts the LinkAccount activity
     * @param view "Link Account" button that was tapped
     */
    public void onLinkAccountClicked(View view) {
        Intent intent = new Intent(this, LinkAccountActivity.class);
        startActivity(intent);
    }

    /**
     * Updates the user display depending on whether a user is signed in and if an account has been linked with the
     * user's account. If the user isn't signed in, the text views will ask the user to sign in. If an account has not
     * been linked, the text views will ask the user to link an account
     */
    public void refreshCurrentUserDisplay() {
        if (currentUser == null) {
            nameTextView.setText(R.string.default_sign_in_message);
            emailTextView.setText(R.string.noUserSignInText);

            signInButton.setText(R.string.sign_in_text);
            linkAccountButton.setVisibility(View.INVISIBLE);
            linkedTextView.setVisibility(View.INVISIBLE);
            linkedToTextView.setVisibility(View.INVISIBLE);

            lastTripTextView.setVisibility(View.INVISIBLE);
            durationTextView.setVisibility(View.INVISIBLE);
            alarmsTextView.setVisibility(View.INVISIBLE);
        } else {
            // Sets the user's display name and email address
            nameTextView.setText(currentUser.getDisplayName());
            emailTextView.setText(currentUser.getEmail());

            // Changes text to "Sign In" as opposed to "Sign Out", and displays link account button
            signInButton.setText(R.string.sign_out_text);
            linkAccountButton.setVisibility(View.VISIBLE);

            // Makes all the linked account views visible
            linkAccountButton.setVisibility(View.VISIBLE);
            linkedTextView.setVisibility(View.VISIBLE);
            linkedToTextView.setVisibility(View.VISIBLE);

            lastTripTextView.setVisibility(View.VISIBLE);
            durationTextView.setVisibility(View.VISIBLE);
            alarmsTextView.setVisibility(View.VISIBLE);

            final DatabaseReference userDatabaseReference = database.getReference("users/" +
                                                                                    currentUser.getUid() + "/child");
            userDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() == null) {
                        // If the user has no linked children, remove the linked user text views
                        linkedTextView.setText(R.string.linkedAccountsEmpty);

                        lastTripTextView.setVisibility(View.INVISIBLE);
                        durationTextView.setVisibility(View.INVISIBLE);
                        alarmsTextView.setVisibility(View.INVISIBLE);
                    } else {
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

                        // Displays the most recent trip information of the linked user
                        final DatabaseReference childTrips = database.getReference("trips/" + childUserID);
                        childTrips.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.getValue() == null) {
                                    Log.d(tag, "Linked user has not gone on any trips");

                                    // If the linked user has not gone on any trips yet, pass in null for trips
                                    updateLinkedAccountInfo(null);
                                } else {
                                    DataSnapshot latestSnapshot = null;
                                    long latestTime = 0;
                                    for (DataSnapshot childTrip : dataSnapshot.getChildren()) {
                                        SimpleDateFormat dateFormat = new SimpleDateFormat(
                                                                        "EEE MMM dd HH:mm:ss z yyyy");
                                        try {
                                            Date date = dateFormat.parse(childTrip.getKey());
                                            long milliseconds = date.getTime();

                                            if (milliseconds > latestTime) {
                                                latestTime = milliseconds;
                                                latestSnapshot = childTrip;
                                            }
                                            System.out.println("Date in milliseconds: " + milliseconds);
                                        } catch (java.text.ParseException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    // latestSnapshot will be null if there have been no trips
                                    updateLinkedAccountInfo(latestSnapshot);
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
        }
    }

    /**
     * Displays the information on the linked account's latest trip
     * @param dataSnapshot contains the linked account's latest trip; includes start time, end time, and alarms set off
     */
    public void updateLinkedAccountInfo(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null) {
            lastTripTextView.setText(R.string.lastTripEmptyText);
            durationTextView.setText("");
            alarmsTextView.setText("");
        } else {
            // Gets the start time of the most recent trip and displays to user
            String lastTripText = getString(R.string.last_trip_text_literal) + dataSnapshot.getKey();
            lastTripTextView.setText(lastTripText);

            // Gets the duration of the most recent trip and displays to user
            if (dataSnapshot.hasChild("start_time") && dataSnapshot.hasChild("end_time")) {
                Long startTime = (Long) dataSnapshot.child("start_time").getValue();
                Long endTime = (Long) dataSnapshot.child("end_time").getValue();

                // Did all of this by hand so that it rounds to the nearest minute;
                // the built-in methods always round down
                long durationSeconds = (endTime - startTime) / MILLISECONDS_IN_ONE_SECOND;
                long minutes = durationSeconds / SECONDS_IN_ONE_MINUTE;

                // 2 isn't a magic number because there's no potential
                // good name for it; it's just the rounding constant
                if (durationSeconds % SECONDS_IN_ONE_MINUTE >= SECONDS_IN_ONE_MINUTE / 2) {
                    minutes++;
                }

                StringBuilder durationText = new StringBuilder(getString(R.string.duration_text_literal) +
                                                                Long.toString(minutes));
                if (minutes == 1) {
                    durationText.append(getString(R.string.minute_text_literal));
                } else {
                    durationText.append(getString(R.string.minutes_text_literal));
                }

                durationTextView.setText(durationText.toString());
            } else {
                durationTextView.setText(R.string.durationEmptyText);
            }

            // Gets the number of alarms set off during the most recent trip and displays to user
            if (dataSnapshot.hasChild("alarms")) {
                long alarms = dataSnapshot.child("alarms").getChildrenCount();
                String alarmsText = getString(R.string.alarms_text_literal) + Long.toString(alarms);
                alarmsTextView.setText(alarmsText);
            } else {
                alarmsTextView.setText(R.string.alarmsEmptyText);
            }
        }
    }

    @Override
    /**
     * Called after the LinkAccountActivity finishes. Signs in the new user if there is one and writes the new user
     * to the database. Also refreshes the user display with the new user's information
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                currentUser = FirebaseAuth.getInstance().getCurrentUser();

                // Writes the new user to the database with their email address (so they can be found for linking)
                final DatabaseReference newUserReference = database.getReference("users/" + currentUser.getUid());
                newUserReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        newUserReference.child("email").setValue(currentUser.getEmail());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

                refreshCurrentUserDisplay();
            } else {
                Log.d(tag, "Sign in failed");

                // Display a toast saying the sign in attempt failed
                Toast.makeText(this, R.string.sign_in_failed_display_text,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
