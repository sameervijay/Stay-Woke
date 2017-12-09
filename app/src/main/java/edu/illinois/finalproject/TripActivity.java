package edu.illinois.finalproject;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;

import edu.illinois.finalproject.camera.CameraSourcePreview;
import edu.illinois.finalproject.camera.GraphicOverlay;

public class TripActivity extends AppCompatActivity {
    private String tag = "TripActivity";

    private TextureView textureView;
    private ImageView imageView;

    private CameraInterface cameraInterface;

    private MediaPlayer mediaPlayer;
    private CountDownTimer alarmTimer;

    private Button pauseTripButton, resumeTripButton, endTripButton;
    private ImageHandler imageHandler;

    private WokeFaceTracker faceTracker;
    private FaceDetector faceDetector;

    private static final int PLAY_SERVICES_UNAVAILABLE_CODE = 9001;
    private static final int ALARM_TIME = 6000;
    private static final int ALARM_INTERVAL = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // To remove the title bar
        setTheme(R.style.AppTheme_NoTitleBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        textureView = (TextureView) findViewById(R.id.mainTextureView);
        imageView = (ImageView) findViewById(R.id.displayImageView);
        pauseTripButton = (Button) findViewById(R.id.pauseTripButton);
        resumeTripButton = (Button) findViewById(R.id.resumeTripButton);
        endTripButton = (Button) findViewById(R.id.endTripButton);

        // Initializes camera interface and surface texture view that shows camera feed
//        cameraInterface = new CameraInterface(this, textureView);
//        imageHandler = new ImageHandler(this, imageView);

        cameraPreview = (CameraSourcePreview) findViewById(R.id.preview);
        graphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        cameraPreview.tripActivity = this;
        createCameraSource();
    }

    private CameraSource cameraSource = null;
    private CameraSourcePreview cameraPreview;
    private GraphicOverlay graphicOverlay;

    @NonNull
    private FaceDetector createFaceDetector(Context context) {
        faceDetector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
//                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(true)
                .setMinFaceSize(0.35f)
                .build();

        faceTracker = new WokeFaceTracker(graphicOverlay, this);
        Detector.Processor<Face> processor = new LargestFaceFocusingProcessor.Builder(faceDetector, faceTracker).build();
        faceDetector.setProcessor(processor);
        return faceDetector;
    }

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

    private void startCameraSource() {
        // Checks if play services are there
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
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }
    @Override
    protected void onPause() {
        super.onPause();
//        CameraDevice cameraDevice = cameraInterface.getCameraDevice();
//        if (cameraDevice != null) {
//            cameraDevice.close();
//        }
//        if (imageTimer != null) {
//            imageTimer.cancel();
//        }
    }

    public void onPauseClicked(View view) {
        pauseTripButton.setVisibility(View.INVISIBLE);
        resumeTripButton.setVisibility(View.VISIBLE);
        endTripButton.setVisibility(View.VISIBLE);
    }

    public void onResumeClicked(View view) {
        pauseTripButton.setVisibility(View.VISIBLE);
        resumeTripButton.setVisibility(View.INVISIBLE);
        endTripButton.setVisibility(View.INVISIBLE);
    }

    public void onEndClicked(View view) {
        if (cameraInterface.getCameraDevice() != null) {
            cameraInterface.getCameraDevice().close();
        }

        // Closes this activity
        finish();
    }

    public void startAlarm() {
        Log.d(tag, "Call to Trip activity start alarm");
        // In case the alarm happens to already be playing, stop it and start a new one
        stopAlarm();

        // Initializes the media player to play sounds and starts it
        mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI);
        mediaPlayer.start();

        // FIREBASE WRITE TO DATABASE

        // Starts the timer to turn the alarm off after 4 seconds
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                alarmTimer = new CountDownTimer(ALARM_TIME, ALARM_INTERVAL) {
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
    }
    public void stopAlarm() {
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
    }
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public ImageHandler getImageHandler() {
        return imageHandler;
    }

    public static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "Woke");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("TripActivity",  "Failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }
}
