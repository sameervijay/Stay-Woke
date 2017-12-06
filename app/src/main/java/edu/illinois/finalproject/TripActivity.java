package edu.illinois.finalproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraDevice;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TripActivity extends AppCompatActivity {
    private String tag = "TripActivity";

    public Bitmap imageBitmap;
    public String photoFilePath;
    private TextureView textureView;
    private ImageView imageView;
    public Uri photoUri;

    private Timer imageTimer;
    private CameraInterface cameraInterface;

    private FaceDetector detector;
    private MediaPlayer mediaPlayer;

    private Button pauseTripButton, resumeTripButton, endTripButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // To remove the title bar
        setTheme(R.style.AppTheme_NoTitleBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        // Following Camera permission request derived from:
        // https://stackoverflow.com/questions/35451833/requesting-camera-permission-with-android-sdk-23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }
        }

        textureView = (TextureView) findViewById(R.id.mainTextureView);
//        imageView = (ImageView) findViewById(R.id.mainImageView);
        pauseTripButton = (Button) findViewById(R.id.pauseTripButton);
        resumeTripButton = (Button) findViewById(R.id.resumeTripButton);
        endTripButton = (Button) findViewById(R.id.endTripButton);

        // Initializes camera interface and surface texture view that shows camera feed
        cameraInterface = new CameraInterface(this, textureView);

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
    }

    public void analyzeImage(File imageFile) {
        Log.d(tag, "Analyzing image");
        photoUri = Uri.fromFile(imageFile);

        try {
            photoFilePath = imageFile.getAbsolutePath();
            // Gets bitmap from the filepath and displays it in the image view
            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            imageView.setImageBitmap(imageBitmap);

            // Runs the mobile vision
            getFeaturesFromMobileVision(imageBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getFeaturesFromMobileVision(Bitmap imageBitmap) {
        Log.d("Google Vision", "Starting Google Mobile Vision");
        Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
        SparseArray<Face> faces = detector.detect(frame);
        Log.d("Google Vision", "Found " + faces.size() + " faces");

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            Log.d("Google Vision", "Left Open: " + Float.toString(face.getIsLeftEyeOpenProbability()));
            Log.d("Google Vision", "Right Open: " + Float.toString(face.getIsRightEyeOpenProbability()));
            Log.d("Google Vision", "Smiling: " + Float.toString(face.getIsSmilingProbability()));

//            for (Landmark landmark : face.getLandmarks()) {
//                Log.d("MainActivity", Integer.toString(landmark.getType()));
//                int cx = (int) (landmark.getPosition().x * scale);
//                int cy = (int) (landmark.getPosition().y * scale);
//                main.drawCircle(cx, cy, 10, paint);
//            }
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
        cameraInterface.getCameraDevice().close();

        // Closes this activity
        finish();
    }

    public static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "StayWoke");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("TripActivity",  "Failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }
}
