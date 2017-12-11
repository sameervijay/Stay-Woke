//package edu.illinois.finalproject;
//
//
//import android.graphics.PointF;
//import android.media.MediaPlayer;
//import android.util.Log;
//
//import com.google.android.gms.vision.Tracker;
//import com.google.android.gms.vision.face.Face;
//import com.google.android.gms.vision.face.FaceDetector;
//import com.google.android.gms.vision.face.Landmark;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import edu.illinois.finalproject.camera.GraphicOverlay;
//
///**
// * Created by sameervijay on 12/9/17.
// */
//
//// Class derived from: Google Vision GooglyEyes Sample
//// https://github.com/googlesamples/android-vision/blob/master/visionSamples/googly-eyes
//
//public class WokeFaceTracker extends Tracker<Face> {
//    private static final float EYES_CLOSED_THRESHOLD = 0.3f;
//    private static final int EYES_CLOSED_ALARM_DELAY = 1000;
//    private static final int FACE_OUT_ALARM_DELAY = 3000;
//    private final String tag = "WokeFaceTracker";
//
//    private GraphicOverlay graphicOverlay;
//    private WokeEyesGraphic eyesGraphic;
//
//    private Map<Integer, PointF> previousProportions = new HashMap<>();
//
//    private boolean lastLeftOpen = true;
//    private boolean lastRightOpen = true;
//
//    private long eyesClosedStartTime = 0;
//    private boolean setOffAlarm = false;
//
//    private TripActivity tripActivity;
//
//    WokeFaceTracker(GraphicOverlay overlay, TripActivity activity) {
//        graphicOverlay = overlay;
//        tripActivity = activity;
//    }
//
//    @Override
//    public void onNewItem(int id, Face face) {
//        eyesGraphic = new WokeEyesGraphic(graphicOverlay);
//    }
//
//    @Override
//    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
//        graphicOverlay.add(eyesGraphic);
//
//        updatePreviousProportions(face);
//
//        PointF leftPosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
//        PointF rightPosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);
//
//        float leftOpenScore = face.getIsLeftEyeOpenProbability();
//        boolean leftOpen;
//        if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
//            leftOpen = lastLeftOpen;
//        } else {
//            leftOpen = (leftOpenScore > EYES_CLOSED_THRESHOLD);
//            lastLeftOpen = leftOpen;
//        }
//
//        float rightOpenScore = face.getIsRightEyeOpenProbability();
//        boolean rightOpen;
//        if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
//            rightOpen = lastRightOpen;
//        } else {
//            rightOpen = (rightOpenScore > EYES_CLOSED_THRESHOLD);
//            lastRightOpen = rightOpen;
//        }
//        Log.d(tag, leftPosition.toString());
//        Log.d(tag, rightPosition.toString());
//        Log.d(tag, "Left open: " + leftOpen);
//        Log.d(tag, "Right open: " + rightOpen);
//
//
//        Log.d(tag, "Eyes closed start time: " + eyesClosedStartTime);
//
//        MediaPlayer mediaPlayer = tripActivity.getMediaPlayer();
//        if (mediaPlayer != null) {
//            Log.d(tag, "Media player playing: " + mediaPlayer.isPlaying());
//        } else {
//            Log.d(tag, "Media player null");
//        }
//
//
//        if (!leftOpen && !rightOpen) {
//            // Starts the eyes closed "timer" because eyes need to be closed for 1 second for an alarm to go off
//            if (eyesClosedStartTime == 0 && (mediaPlayer == null || !mediaPlayer.isPlaying())) {
//                Log.d(tag, "Starting eyes closed timer");
//                eyesClosedStartTime = System.currentTimeMillis();
//            } else if (System.currentTimeMillis() > eyesClosedStartTime + EYES_CLOSED_ALARM_DELAY && !setOffAlarm &&
//                        (mediaPlayer == null || !mediaPlayer.isPlaying())) {
//                Log.d(tag, "Starting alarm");
//
//                // setOffAlarm exists to ensure another alarm doesn't go off after the first one ends
//                setOffAlarm = true;
//                tripActivity.startAlarm();
//            }
//        } else {
//            Log.d(tag, "Stopping alarm and eyes closed timer");
//            eyesClosedStartTime = 0;
//            setOffAlarm = false;
//            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//                tripActivity.stopAlarm();
//            }
//        }
//
//        // Draws the bounding boxes around eyes
//        eyesGraphic.updateEyes(leftPosition, leftOpen, rightPosition, rightOpen);
//    }
//
//    @Override
//    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
//        MediaPlayer mediaPlayer = tripActivity.getMediaPlayer();
//
//        // Starts the eyes closed "timer" because eyes need to be closed for 1 second for an alarm to go off
//        if (eyesClosedStartTime == 0 && (mediaPlayer == null || !mediaPlayer.isPlaying())) {
//            Log.d(tag, "Starting eyes closed timer");
//            eyesClosedStartTime = System.currentTimeMillis();
//        } else if (System.currentTimeMillis() > eyesClosedStartTime + FACE_OUT_ALARM_DELAY && !setOffAlarm &&
//                (mediaPlayer == null || !mediaPlayer.isPlaying())) {
//            Log.d(tag, "Starting alarm");
//
//            // setOffAlarm exists to ensure another alarm doesn't go off after the first one ends
//            setOffAlarm = true;
//            tripActivity.startAlarm();
//        }
//
//        graphicOverlay.remove(eyesGraphic);
//    }
//
//    @Override
//    public void onDone() {
//        graphicOverlay.remove(eyesGraphic);
//    }
//
//    private void updatePreviousProportions(Face face) {
//        for (Landmark landmark : face.getLandmarks()) {
//            PointF position = landmark.getPosition();
//            float xProp = (position.x - face.getPosition().x) / face.getWidth();
//            float yProp = (position.y - face.getPosition().y) / face.getHeight();
//            previousProportions.put(landmark.getType(), new PointF(xProp, yProp));
//        }
//    }
//
//    /**
//     * Finds a specific landmark position, or approximates the position based on past observations
//     * if it is not present.
//     */
//    private PointF getLandmarkPosition(Face face, int landmarkId) {
//        for (Landmark landmark : face.getLandmarks()) {
//            if (landmark.getType() == landmarkId) {
//                return landmark.getPosition();
//            }
//        }
//
//        PointF prop = previousProportions.get(landmarkId);
//        if (prop == null) {
//            return null;
//        }
//
//        float x = face.getPosition().x + (prop.x * face.getWidth());
//        float y = face.getPosition().y + (prop.y * face.getHeight());
//        return new PointF(x, y);
//    }
//}
//
