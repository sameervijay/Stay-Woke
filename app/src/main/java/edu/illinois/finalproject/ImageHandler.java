package edu.illinois.finalproject;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

/**
 * Created by sameervijay on 12/2/17.
 */

public class ImageHandler {
    private MainActivity mainActivity;

    public ImageHandler(MainActivity activity) {
        mainActivity = activity;

        FaceDetector detector = new FaceDetector.Builder(mainActivity).setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
    }
}
