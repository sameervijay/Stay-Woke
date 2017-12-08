package edu.illinois.finalproject;

import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.IOException;

/**
 * Created by sameervijay on 12/2/17.
 */

public class ImageHandler {
    private TripActivity tripActivity;
//    private FaceDetector detector;

    private Uri photoUri;
    private Bitmap imageBitmap;
    private String photoFilePath;
    private String tag = "ImageHandler";

    private ImageView displayImageView;

    public ImageHandler(TripActivity activity, ImageView displayView) {
        tripActivity = activity;
        displayImageView = displayView;

//        detector = new FaceDetector.Builder(tripActivity).setTrackingEnabled(false)
//                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
//                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
//                .build();
    }

    public void analyzeImage(File imageFile) {
        Log.d(tag, "Analyzing image");
        photoUri = Uri.fromFile(imageFile);

        try {
            photoFilePath = imageFile.getAbsolutePath();
            // Gets bitmap from the filepath and displays it in the image view
            imageBitmap = MediaStore.Images.Media.getBitmap(tripActivity.getContentResolver(), photoUri);
            displayImageView.setImageBitmap(imageBitmap);

            // Runs the mobile vision
//            getFeaturesFromMobileVision(imageBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void getFeaturesFromMobileVision(Bitmap imageBitmap) {
//        Log.d("Google Vision", "Starting Google Mobile Vision");
//        Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
//        SparseArray<Face> faces = detector.detect(frame);
//        Log.d("Google Vision", "Found " + faces.size() + " faces");
//
//        for (int i = 0; i < faces.size(); ++i) {
//            Face face = faces.valueAt(i);
//            Log.d("Google Vision", "Left Open: " + Float.toString(face.getIsLeftEyeOpenProbability()));
//            Log.d("Google Vision", "Right Open: " + Float.toString(face.getIsRightEyeOpenProbability()));
//            Log.d("Google Vision", "Smiling: " + Float.toString(face.getIsSmilingProbability()));
//
////            for (Landmark landmark : face.getLandmarks()) {
////                Log.d("MainActivity", Integer.toString(landmark.getType()));
////                int cx = (int) (landmark.getPosition().x * scale);
////                int cy = (int) (landmark.getPosition().y * scale);
////                main.drawCircle(cx, cy, 10, paint);
////            }
//        }
//    }
}
