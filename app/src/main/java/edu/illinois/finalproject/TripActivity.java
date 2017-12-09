package edu.illinois.finalproject;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

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
//        cameraInterface = new CameraInterface(this, textureView);
//        imageHandler = new ImageHandler(this, imageView);

        // Initializes the media player to play sounds
        mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI);

//        imageTimer = new Timer();
//        imageTimer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                cameraInterface.getPicture();
//            }
//        }, 0, 5000);

        cameraPreview = (CameraSourcePreview) findViewById(R.id.preview);
        graphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        cameraPreview.tripActivity = this;
        createCameraSource();
    }

    private CameraSource cameraSource = null;
    private CameraSourcePreview cameraPreview;
    private GraphicOverlay graphicOverlay;

    /**
     * Creates the face detector and associated processing pipeline to support either front facing
     * mode or rear facing mode.  Checks if the detector is ready to use, and displays a low storage
     * warning if it was not possible to download the face library.
     */
    @NonNull
    private FaceDetector createFaceDetector(Context context) {
        // Use of "fast mode" enables faster detection for frontward faces, at the expense of not
        // attempting to detect faces at more varied angles (e.g., faces in profile).  Therefore,
        // faces that are turned too far won't be detected under fast mode.
        //
        // Setting the minimum face size not only controls how large faces must be in order to be
        // detected, it also affects performance.  Since it takes longer to scan for smaller faces,
        // we increase the minimum face size for the rear facing mode a little bit in order to make
        // tracking faster (at the expense of missing smaller faces).  But this optimization is less
        // important for the front facing case, because when "prominent face only" is enabled, the
        // detector stops scanning for faces after it has found the first (large) face.
        FaceDetector detector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
//                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(true)
                .setMinFaceSize(0.35f)
                .build();

        Tracker<Face> tracker = new WokeFaceTracker(graphicOverlay);
        Detector.Processor<Face> processor = new LargestFaceFocusingProcessor.Builder(detector, tracker).build();
        detector.setProcessor(processor);
        return detector;
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

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        // check that the device has play services available.
//        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
//        if (code != ConnectionResult.SUCCESS) {
//            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
//            dlg.show();
//        }

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

    public ImageHandler getImageHandler() {
        return imageHandler;
    }

    public void processFace(FaceData face) {
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
