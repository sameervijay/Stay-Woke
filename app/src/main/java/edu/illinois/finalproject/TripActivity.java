package edu.illinois.finalproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TripActivity extends AppCompatActivity {
    private String tag = "TripActivity";

    private TextureView textureView;
    private ImageView imageView;

    private Timer imageTimer;
    private CameraInterface cameraInterface;
    private FaceData lastFace;

    private MediaPlayer mediaPlayer;
    private Button pauseTripButton, resumeTripButton, endTripButton;
    private ImageHandler imageHandler;

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
        cameraInterface = new CameraInterface(this, textureView);
        imageHandler = new ImageHandler(this, imageView);

        // Initializes the media player to play sounds
        mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI);

        imageTimer = new Timer();
        imageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cameraInterface.getPicture();
            }
        }, 0, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraDevice cameraDevice = cameraInterface.getCameraDevice();
        if (cameraDevice != null) {
            cameraDevice.close();
        }
        if (imageTimer != null) {
            imageTimer.cancel();
        }
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

    public ImageHandler getImageHandler() {
        return imageHandler;
    }

    public void processFace(FaceData face) {
        // Draws bounding boxes over face and eyes
        redrawFace(face);

        if (face == null) {
            return;
        }

        // These constants are subject to change
        if (FaceData.leftEyeOpenThreshold == 0) {
            FaceData.leftEyeOpenThreshold = face.getLeftEyeOpenProb() * 0.75f;
        }
        if (FaceData.rightEyeOpenThreshold == 0) {
            FaceData.rightEyeOpenThreshold = face.getRightEyeOpenProb() * 0.75f;
        }

        lastFace = face;
    }

    public void redrawFace(FaceData face) {
        // If face is null (no face is detected), draw a red X mark with a general tint too
        if (face == null) {

        } else {
            // Draw bounding boxes on the face
        }
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
