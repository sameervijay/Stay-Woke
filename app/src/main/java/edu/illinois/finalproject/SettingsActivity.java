package edu.illinois.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private String name, email, linkedEmail;
    private boolean isSignedIn;

    private Button signInButton, linkAccountButton, createAccountButton;
    private TextView nameTextView, emailTextView;

    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser = firebaseAuth.getCurrentUser();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        signInButton = (Button) findViewById(R.id.signInButton);
        linkAccountButton = (Button) findViewById(R.id.linkAccountButton);
        nameTextView = (TextView) findViewById(R.id.nameTextView);
        emailTextView = (TextView) findViewById(R.id.emailTextView);

        refreshCurrentUserDisplay();
    }

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

    public void onLinkAccountClicked(View view) {
        Intent intent = new Intent(this, LinkAccountActivity.class);
        startActivity(intent);
    }

    public void refreshCurrentUserDisplay() {
        if (currentUser == null) {
            nameTextView.setText(R.string.default_sign_in_message);
            emailTextView.setText("");

            // ADD TEXT VIEWS FOR LINKED USERS

            signInButton.setText(R.string.sign_in_text);
            linkAccountButton.setVisibility(View.INVISIBLE);
        } else {
            nameTextView.setText(currentUser.getDisplayName());
            emailTextView.setText(currentUser.getEmail());

            signInButton.setText(R.string.sign_out_text);
            linkAccountButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                currentUser = FirebaseAuth.getInstance().getCurrentUser();

                refreshCurrentUserDisplay();
            } else {
                // Sign in failed, check response for error code
            }
        }
    }
}
