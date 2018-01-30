package edu.illinois.finalproject;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;
import java.util.Calendar;

import edu.illinois.finalproject.camera.CameraSourcePreview;
import edu.illinois.finalproject.camera.GraphicOverlay;

public class TripActivity extends AppCompatActivity {
    private final String tag = "TripActivity";

    private static final int PLAY_SERVICES_UNAVAILABLE_CODE = 9001;
    private static final int ALARM_DURATION_MILLISECONDS = 8000;
    private static final int ALARM_INTERVAL_MILLISECONDS = 1000;

    private MediaPlayer mediaPlayer;
    private CountDownTimer alarmTimer;

    private Button pauseTripButton, resumeTripButton, endTripButton;
    private ImageView pauseTripImageView;
    private TextView durationTextView, durationLiteralTextView, alertTextView, alertLiteralTextView;

    private WokeFaceTracker faceTracker;
    private FaceDetector faceDetector;
    private CameraSource cameraSource = null;
    private CameraSourcePreview cameraPreview;
    private GraphicOverlay graphicOverlay;

//    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
//    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private String tripStartTime;
    private String alarmStartTime;
    private int alerts;
    private long startTimeLong;

    private ImageView noFaceBackground;
    private TextView noFaceTextView;
    private boolean showingNoFaceMessage;
    private boolean inCalibrationPeriod;

    private boolean isPaused;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // To remove the title bar
        setTheme(R.style.AppTheme_NoTitleBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        pauseTripButton = (Button) findViewById(R.id.pauseTripButton);
        resumeTripButton = (Button) findViewById(R.id.resumeTripButton);
        endTripButton = (Button) findViewById(R.id.endTripButton);
        pauseTripImageView = (ImageView) findViewById(R.id.pauseTripBackground);
        durationTextView = (TextView) findViewById(R.id.durationTextView);
        durationLiteralTextView = (TextView) findViewById(R.id.durationLiteralTextView);
        alertTextView = (TextView) findViewById(R.id.alertsTextView);
        alertLiteralTextView = (TextView) findViewById(R.id.alertsLiteralTextView);

        noFaceBackground = (ImageView) findViewById(R.id.noFaceBackground);
        noFaceTextView = (TextView) findViewById(R.id.noFaceTextView);

        // Initializes camera interface and surface texture view that shows camera feed
        cameraPreview = (CameraSourcePreview) findViewById(R.id.preview);
        graphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        cameraPreview.tripActivity = this;
        createCameraSource();

        inCalibrationPeriod = true;
        showingNoFaceMessage = false;

        isPaused = false;
        alerts = 0;
        startTimeLong = System.currentTimeMillis();

        // Request camera permission if it has not already been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        }

        // Database write that a trip is starting
//        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//        if (currentUser != null) {
//            String currentTime = Calendar.getInstance().getTime().toString();
//            final DatabaseReference databaseReference = database.getReference("trips/" + currentUser.getUid()
//                                                                                    + "/" + currentTime);
//            tripStartTime = currentTime;
//
//            final long startTime = Calendar.getInstance().getTimeInMillis();
//            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//                    databaseReference.child("start_time").setValue(startTime);
//                    databaseReference.child("in_progress").setValue(true);
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                }
//            });
//        } else {
//            Log.d(tag, "Couldn't write trip to Firebase because user isn't signed in");
//        }
    }

    public void showNoFaceCalibrationMessage() {
        Log.d(tag, "No face detected during calibration period");
        showingNoFaceMessage = true;
        noFaceTextView.setVisibility(View.VISIBLE);
        noFaceBackground.setVisibility(View.VISIBLE);
    }
    public void removeNoFaceCalibrationMessage() {
        Log.d(tag, "Face detected during calibration period");
        showingNoFaceMessage = false;
        noFaceTextView.setVisibility(View.INVISIBLE);
        noFaceBackground.setVisibility(View.INVISIBLE);
    }
    public boolean isShowingNoFaceMessage() {
        return showingNoFaceMessage;
    }
    public boolean isInCalibrationPeriod() {
        return inCalibrationPeriod;
    }
    public void exitCalibrationPeriod() {
        Log.d(tag, "EXITING calibration period");
        inCalibrationPeriod = false;
    }

    @NonNull
    /**
     * Instantiates the faceDetector instance variable with tracking enabled to speed up processing between frames,
     * classification enabled to determine whether eyes are open or closed, prominent face only set to true to expect
     * only one face in the image to speed up processing, landmark type to all landmarks to look for all facial
     * features, and minimum face size to speed up processing by not looking for faces that are too small
     */
    private FaceDetector createFaceDetector(Context context) {
        faceDetector = new FaceDetector.Builder(context).setTrackingEnabled(true)
                                                        .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                                                        .setProminentFaceOnly(true)
                                                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                                                        .setMinFaceSize(0.25f)
                                                        .build();

        faceTracker = new WokeFaceTracker(graphicOverlay, this);
        Detector.Processor<Face> processor = new LargestFaceFocusingProcessor
                                                                    .Builder(faceDetector, faceTracker).build();
        faceDetector.setProcessor(processor);
        return faceDetector;
    }

    /**
     * Initializes the cameraSource instance variable with 30fps, 320x240 px resolution, facing front, and autofocus
     * enabled. Also creates the FaceDetector object and passes that into the CameraSource
     */
    private void createCameraSource() {
        Context context = getApplicationContext();
        FaceDetector detector = createFaceDetector(context);

        int facing = CameraSource.CAMERA_FACING_FRONT;

        cameraSource = new CameraSource.Builder(context, detector)
                .setFacing(facing)
                .setRequestedPreviewSize(320, 240)
                .setRequestedFps(30.0f)
                .setAutoFocusEnabled(true)
                .build();
    }

    /**
     * Actually starts the cameraPreview, passing in the non-null cameraSource and graphicOverlay
     */
    private void startCameraSource() {
        // Checks if Google play services are available
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this,
                                                                            code, PLAY_SERVICES_UNAVAILABLE_CODE);
            dlg.show();
        }

        if (cameraSource != null) {
            try {
                cameraPreview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(tag, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    /**
     * Starts the camera preview again when the app is in the foreground
     */
    protected void onResume() {
        super.onResume();
        if (!isPaused) {
            startCameraSource();
        }
    }
    @Override
    /**
     * Stops the cameraPreview when the app is in the background
     */
    protected void onPause() {
        super.onPause();
        if (cameraPreview != null) {
            cameraPreview.stop();
        }
    }
    @Override
    /**
     * Releases the cameraSource right before the activity is destroyed
     */
    protected void onDestroy() {
        super.onDestroy();

        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    /**
     * Called when the "Pause Trip" button is pressed in the TripActivity.
     * @param view "Pause Trip" button
     */
    public void onPauseClicked(View view) {
        alertTextView.setText(Integer.toString(alerts));
        // 3800 seconds --> 1:03:20
        int hours = (int) (System.currentTimeMillis() - startTimeLong) / 1000 / 3600;
        int seconds = (int) (System.currentTimeMillis() - startTimeLong) / 1000 % 60;
        int minutes = (int) ((System.currentTimeMillis() - startTimeLong) / 1000 % 3600) / 60;

        String duration = Long.toString(hours) + ":";
        if (minutes < 10) {
            duration += "0";
        }
        duration += minutes + ":";
        if (seconds < 10) {
            duration += "0";
        }
        duration += seconds;
        durationTextView.setText(duration);

        pauseTripButton.setVisibility(View.INVISIBLE);
        resumeTripButton.setVisibility(View.VISIBLE);
        pauseTripImageView.setVisibility(View.VISIBLE);
        alertTextView.setVisibility(View.VISIBLE);
        alertLiteralTextView.setVisibility(View.VISIBLE);
        durationTextView.setVisibility(View.VISIBLE);
        durationLiteralTextView.setVisibility(View.VISIBLE);
        isPaused = true;

        if (cameraPreview != null) {
            cameraPreview.stop();
        }
        stopAlarm();
    }

    /**
     * Called when the "Resume Trip" button is pressed in the TripActivity.
     * @param view "Resume Trip" button
     */
    public void onResumeClicked(View view) {
        pauseTripButton.setVisibility(View.VISIBLE);
        resumeTripButton.setVisibility(View.INVISIBLE);
        pauseTripImageView.setVisibility(View.INVISIBLE);
        alertTextView.setVisibility(View.INVISIBLE);
        alertLiteralTextView.setVisibility(View.INVISIBLE);
        durationTextView.setVisibility(View.INVISIBLE);
        durationLiteralTextView.setVisibility(View.INVISIBLE);
        isPaused = false;

        startCameraSource();
    }

    /**
     * Called when the "End Trip" button is pressed in the TripActivity.
     * @param view "End Trip" button
     */
    public void onEndClicked(View view) {
        // Firebase database write for trip ending
//        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//
//        if (currentUser != null && tripStartTime != null) {
//            final DatabaseReference databaseReference = database.getReference("trips/" + currentUser.getUid() +
//                    "/" + tripStartTime);
//            final long endTime = Calendar.getInstance().getTimeInMillis();
//            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//                    databaseReference.child("end_time").setValue(endTime);
//                    databaseReference.child("in_progress").setValue(false);
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                }
//            });
//
//            tripStartTime = null;
//        } else {
//            Log.d(tag, "Couldn't write trip to Firebase because user isn't signed in");
//        }

        // Closes this instance of TripActivity
        finish();
    }

    /**
     * Called when the settings button is clicked
     * @param view settings button that was clicked
     */
    public void onSettingsClicked(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);

        if (cameraPreview != null) {
            cameraPreview.stop();
        }
    }
    @Override
    /**
     * Called when the SettingsActivity closes; restarts the camera preview
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (cameraPreview != null) {
            startCameraSource();
        }
    }

    /**
     * Called when an alarm goes off. Initializes the mediaPlayer to play the sound and begins playing it, starts a
     * timer on a new thread to stop the alarm after ALARM_DURATION_MILLISECONDS, and writes to the database the time
     * at which the alarm was started
     */
    public void startAlarm() {
        Log.d(tag, "Call to Trip activity start alarm");
        alerts++;

        // Initializes the media player to play sounds and starts it
        mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        mediaPlayer.start();

        // In main thread, starts the timer to turn the alarm off after ALARM_DURATION_MILLISECONDS seconds
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                alarmTimer = new CountDownTimer(ALARM_DURATION_MILLISECONDS, ALARM_INTERVAL_MILLISECONDS) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        stopAlarm();
                    }
                }.start();
            }
        });

        // Firebase database write that alarm is starting
//        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//        if (currentUser != null) {
//            final DatabaseReference databaseReference = database.getReference("trips/" + currentUser.getUid() +
//                    "/" + tripStartTime +
//                    "/alarms");
//
//            final String currentTimeText = Calendar.getInstance().getTime().toString();
//            final long currentTimeNum = Calendar.getInstance().getTimeInMillis();
//            alarmStartTime = currentTimeText;
//
//            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//                    databaseReference.child(currentTimeText).child("alarm_start").setValue(currentTimeNum);
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                }
//            });
//        } else {
//            Log.d(tag, "Couldn't write trip to Firebase because user isn't signed in");
//        }
    }

    /**
     * Called when an alarm is supposed to be stopped. Stops the mediaPlayer and writes to the database the time at
     * which the alarm was stopped
     */
    public void stopAlarm() {

        // Stop the media player and alarm timer if they are not null
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
        mediaPlayer = null;

        if (alarmTimer != null) {
            alarmTimer.cancel();
        }
        alarmTimer = null;

        // Write to Firebase the time the alarm has ended
//        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//        if (currentUser != null && alarmStartTime != null) {
//            final DatabaseReference databaseReference = database.getReference("trips/" + currentUser.getUid() +
//                    "/" + tripStartTime +
//                    "/alarms" + "/" + alarmStartTime);
//
//            final long currentTimeNum = Calendar.getInstance().getTimeInMillis();
//            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//                    databaseReference.child("alarm_stop").setValue(currentTimeNum);
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                }
//            });
//        } else {
//            Log.d(tag, "Couldn't write alarm ending to Firebase b/c user isn't signed in or alarm " +
//                    "has already ended");
//        }

        alarmStartTime = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                finish();

                Toast.makeText(this, "To begin eye detection, Stay Woke needs permission to use the camera. Please" +
                        " try again.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
    public Button getPauseTripButton() {
        return pauseTripButton;
    }
    public long getStartTimeLong() {
        return startTimeLong;
    }

}
