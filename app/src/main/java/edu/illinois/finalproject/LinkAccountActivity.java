package edu.illinois.finalproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.firebase.FirebaseApp;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LinkAccountActivity extends AppCompatActivity {
    private final String tag = "LinkAccountActivity";

    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    private TextView linkAccountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_account);

        linkAccountTextView = (TextView) findViewById(R.id.linkAccountTextView);
    }

    public void onLinkRequestClicked(View view) {
        final String userInput = linkAccountTextView.getText().toString();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null ) {
            final DatabaseReference databaseReference = database.getReference("users");
            final LinkAccountActivity activity = this;
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot user : dataSnapshot.getChildren()) {
                        if (user.hasChild("email")) {
                            String email = (String)user.child("email").getValue();

                            if (email.equals(userInput)) {
                                // Actually writes the linkage between current user and child to database
                                writeLinkedUserToFirebase(user.getKey());

                                // Link completes successfully
                                Toast.makeText(activity, "Your account is linked with " + email,
                                        Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }
                        }
                    }

                    // If no user is found, display toast saying that
                    Toast.makeText(activity, "No user was found with that email address",
                                    Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else {
            Log.d(tag, "User is not signed in");
        }


    }

    public void writeLinkedUserToFirebase(final String linkedUserID) {
        // Writes child for current user
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        final DatabaseReference databaseReference = database.getReference("users/" + currentUser.getUid());

        databaseReference.child("child").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                databaseReference.child("child").setValue(linkedUserID);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Writes parent for linked user
        final DatabaseReference databaseReferenceLinked = database.getReference("users/" + linkedUserID);

        final String parentToAdd = currentUser.getUid();
        databaseReferenceLinked.child("parent").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                databaseReferenceLinked.child("parent").setValue(parentToAdd);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}
