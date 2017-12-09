package edu.illinois.finalproject;

import android.graphics.PointF;
import android.util.Log;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.util.HashMap;
import java.util.Map;

import edu.illinois.finalproject.camera.GraphicOverlay;

/**
 * Created by sameervijay on 12/9/17.
 */

// Following class derived from:
// https://github.com/googlesamples/android-vision/blob/master/visionSamples/googly-eyes/app/src/main/java/com/google/
// android/gms/samples/vision/face/googlyeyes/GooglyFaceTracker.java

public class WokeFaceTracker extends Tracker<Face> {
    private static final float EYE_CLOSED_THRESHOLD = 0.3f;
    private final String tag = "WokeFaceTracker";

    private GraphicOverlay graphicOverlay;
    private WokeEyesGraphic eyesGraphic;

    private Map<Integer, PointF> previousProportions = new HashMap<>();

    private boolean lastLeftOpen = true;
    private boolean lastRightOpen = true;

    WokeFaceTracker(GraphicOverlay overlay) {
        graphicOverlay = overlay;
    }

    @Override
    public void onNewItem(int id, Face face) {
        eyesGraphic = new WokeEyesGraphic(graphicOverlay);
    }

    /**
     * Updates the positions and state of eyes to the underlying graphic, according to the most
     * recent face detection results.  The graphic will render the eyes and simulate the motion of
     * the iris based upon these changes over time.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
        graphicOverlay.add(eyesGraphic);

        updatePreviousProportions(face);

        PointF leftPosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
        PointF rightPosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);

        float leftOpenScore = face.getIsLeftEyeOpenProbability();
        boolean isLeftOpen;
        if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            isLeftOpen = lastLeftOpen;
        } else {
            isLeftOpen = (leftOpenScore > EYE_CLOSED_THRESHOLD);
            lastLeftOpen = isLeftOpen;
        }

        float rightOpenScore = face.getIsRightEyeOpenProbability();
        boolean isRightOpen;
        if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            isRightOpen = lastRightOpen;
        } else {
            isRightOpen = (rightOpenScore > EYE_CLOSED_THRESHOLD);
            lastRightOpen = isRightOpen;
        }
        Log.d(tag, leftPosition.toString());
        Log.d(tag, rightPosition.toString());;
        Log.d(tag, "Left open: " + isLeftOpen);
        Log.d(tag, "Right open: " + isRightOpen);

        // Draws the bounding boxes around eyes
        eyesGraphic.updateEyes(leftPosition, isLeftOpen, rightPosition, isRightOpen);
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily (e.g., if the face was momentarily blocked from
     * view).
     */
    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        graphicOverlay.remove(eyesGraphic);
    }

    /**
     * Called when the face is assumed to be gone for good. Remove the googly eyes graphic from
     * the overlay.
     */
    @Override
    public void onDone() {
        graphicOverlay.remove(eyesGraphic);
    }

    private void updatePreviousProportions(Face face) {
        for (Landmark landmark : face.getLandmarks()) {
            PointF position = landmark.getPosition();
            float xProp = (position.x - face.getPosition().x) / face.getWidth();
            float yProp = (position.y - face.getPosition().y) / face.getHeight();
            previousProportions.put(landmark.getType(), new PointF(xProp, yProp));
        }
    }

    /**
     * Finds a specific landmark position, or approximates the position based on past observations
     * if it is not present.
     */
    private PointF getLandmarkPosition(Face face, int landmarkId) {
        for (Landmark landmark : face.getLandmarks()) {
            if (landmark.getType() == landmarkId) {
                return landmark.getPosition();
            }
        }

        PointF prop = previousProportions.get(landmarkId);
        if (prop == null) {
            return null;
        }

        float x = face.getPosition().x + (prop.x * face.getWidth());
        float y = face.getPosition().y + (prop.y * face.getHeight());
        return new PointF(x, y);
    }
}
