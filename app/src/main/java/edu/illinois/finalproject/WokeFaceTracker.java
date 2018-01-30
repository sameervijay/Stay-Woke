package edu.illinois.finalproject;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.media.MediaPlayer;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.illinois.finalproject.camera.GraphicOverlay;

/**
 * Created by sameervijay on 12/9/17.
 */

// Class derived from: Google Vision GooglyEyes Sample
// https://github.com/googlesamples/android-vision/blob/master/visionSamples/googly-eyes

public class WokeFaceTracker extends Tracker<Face> {
    private float leftEyeClosedThreshold = 0.35f;
    private float rightEyeClosedThreshold = 0.35f;
    private int eyesClosedAlarmDelay = 1000;
    private int faceOutAlarmDelay = 3000;
    private static final int CALIBRATION_FRAMES_TO_KEEP = 150;
    private static final int NORMAL_FRAMES_TO_KEEP = 8;
    private final String tag = "WokeFaceTracker";

    private GraphicOverlay graphicOverlay;
    private WokeEyesGraphic eyesGraphic;

    private Map<Integer, PointF> previousProportions = new HashMap<>();

    private boolean lastLeftOpen = true;
    private boolean lastRightOpen = true;

    private long eyesClosedStartTime = 0;
    private boolean setOffAlarm = false;
    private boolean alertForMissingEyes = true;

    private boolean finishedLoadingScreen = false;

    private ArrayList<WokeFrame> recentFrames = new ArrayList<>();
    private TripActivity tripActivity;

    WokeFaceTracker(GraphicOverlay overlay, TripActivity activity) {
        graphicOverlay = overlay;
        tripActivity = activity;
        eyesGraphic = new WokeEyesGraphic(graphicOverlay, tripActivity);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        tripActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        eyesGraphic.deviceWidth = displayMetrics.widthPixels;

        SharedPreferences sharedPref = tripActivity.getPreferences(Context.MODE_PRIVATE);
        if (sharedPref.contains("AlertDelay")) {
            eyesClosedAlarmDelay = sharedPref.getInt("AlarmDelay", 1000);
        }
        if (sharedPref.contains("AlertForMissingEyes")) {
            alertForMissingEyes = sharedPref.getBoolean("AlertForMissingEyes", true);
        }
    }

    @Override
    /**
     * Called when a new face is detected. Initializes eyesGraphic which contains the overlaid bounding boxes
     */
    public void onNewItem(int id, Face face) {
//        eyesGraphic = new WokeEyesGraphic(graphicOverlay);
    }

    @Override
    /**
     * Called every time the tracker updates/processes a new frame. Starts the alarm if both eyes are closed
     *
     * @param detectionResults contains the detailed results of the tracker's face detection
     * @param face Face object containing landmarks and landmark positions
     */
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
        graphicOverlay.add(eyesGraphic);

        updatePreviousProportions(face);

        // Adds the frame to the list of recent frames
        WokeFrame frame = new WokeFrame(System.currentTimeMillis(), true,
                face.getIsLeftEyeOpenProbability(), face.getIsRightEyeOpenProbability());
        recentFrames.add(frame);

        PointF leftPosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
        PointF rightPosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);

        // Finds the average eye-open probabilities from recentFrames
        float avgRecentLeftOpenProb = 0.0f;
        float avgRecentRightOpenProb = 0.0f;
        for (WokeFrame wokeFrame : recentFrames) {
            avgRecentLeftOpenProb += wokeFrame.getLeftOpenProb();
            avgRecentRightOpenProb += wokeFrame.getRightOpenProb();
        }
        avgRecentLeftOpenProb /= recentFrames.size();
        avgRecentRightOpenProb /= recentFrames.size();
        System.out.println("LeftOpen: " + avgRecentLeftOpenProb);
        System.out.println("RightOpen: " + avgRecentLeftOpenProb);

        float leftOpenScore = face.getIsLeftEyeOpenProbability();
        float rightOpenScore = face.getIsRightEyeOpenProbability();
        boolean leftOpen, rightOpen;
        if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            leftOpen = lastLeftOpen;
        } else {
            leftOpen = (avgRecentLeftOpenProb > leftEyeClosedThreshold);
            lastLeftOpen = leftOpen;
        }
        if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            rightOpen = lastRightOpen;
        } else {
            rightOpen = (avgRecentRightOpenProb > rightEyeClosedThreshold);
            lastRightOpen = rightOpen;
        }

        if (tripActivity.isInCalibrationPeriod()) {

            // Checks if there have been 60 consecutive frames with a face; if so, exit calibration period
            if (recentFrames.size() >= CALIBRATION_FRAMES_TO_KEEP) {
                eyesGraphic.inCalibrationMode = false;
                tripActivity.exitCalibrationPeriod();

                // Calculates thresholds for eyes being closed relative to standard open-eye values
                leftEyeClosedThreshold = 0;
                rightEyeClosedThreshold = 0;
                for (WokeFrame wokeFrame : recentFrames) {
                    leftEyeClosedThreshold += wokeFrame.getLeftOpenProb();
                    rightEyeClosedThreshold += wokeFrame.getRightOpenProb();
                }
                leftEyeClosedThreshold /= recentFrames.size();
                rightEyeClosedThreshold /= recentFrames.size();

                leftEyeClosedThreshold *= 0.5;
                rightEyeClosedThreshold *= 0.5;
                System.out.println("LEFT THRESHOLD: " + leftEyeClosedThreshold);
                System.out.println("RIGHT THRESHOLD: " + rightEyeClosedThreshold);

                recentFrames.clear();
            }
        } else {
            // If not in calibration mode, start only storing the latest 10 frames
            if (recentFrames.size() >= NORMAL_FRAMES_TO_KEEP) {
                recentFrames.remove(0);
            }

            MediaPlayer mediaPlayer = tripActivity.getMediaPlayer();
            if (mediaPlayer != null) {
                Log.d(tag, "Media player playing: " + mediaPlayer.isPlaying());
            } else {
                Log.d(tag, "Media player null");
            }

            if (!leftOpen && !rightOpen) {
                // Starts the eyes closed "timer" because eyes need to be closed for 1 second for an alarm to go off
                if (eyesClosedStartTime == 0 && (mediaPlayer == null || !mediaPlayer.isPlaying())) {
                    Log.d(tag, "Starting eyes closed timer");
                    eyesClosedStartTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() > eyesClosedStartTime + eyesClosedAlarmDelay && !setOffAlarm &&
                        (mediaPlayer == null || !mediaPlayer.isPlaying())) {

                    Log.d(tag, "Starting alarm");

                    // setOffAlarm exists to ensure another alarm doesn't go off after the first one ends
                    setOffAlarm = true;
                    tripActivity.startAlarm();
                }
            } else {
                Log.d(tag, "Stopping alarm and eyes closed timer");
                eyesClosedStartTime = 0;
                setOffAlarm = false;
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    tripActivity.stopAlarm();
                }
            }
        }

        // Draws the bounding boxes around eyes
        eyesGraphic.updateEyes(leftPosition, rightPosition, leftOpen, rightOpen);
    }

    @Override
    /**
     * Called when the FaceTracker doesn't detect a face; this sets off an alarm, as if the eyes were closed
     */
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        // To prevent the "eyes not detected" message from showing right when a trip starts
        if (finishedLoadingScreen) {
            graphicOverlay.add(eyesGraphic);
            eyesGraphic.updateEyesMissingText();
        } else if (System.currentTimeMillis() - tripActivity.getStartTimeLong() > 200) {
            finishedLoadingScreen = true;
        }

        if (tripActivity.isInCalibrationPeriod()) {
            // Clear the recent frames where the face was detected
            if (recentFrames.size() > 0) {
                recentFrames.clear();
            }
            return;
//            if (!tripActivity.isShowingNoFaceMessage()) {
//                tripActivity.showNoFaceCalibrationMessage();
//            }
        }

        if (alertForMissingEyes) {
            MediaPlayer mediaPlayer = tripActivity.getMediaPlayer();

            // Starts the eyes closed "timer" because eyes need to be closed for 1 second for an alarm to go off
            if (eyesClosedStartTime == 0 && (mediaPlayer == null || !mediaPlayer.isPlaying())) {
                Log.d(tag, "Starting eyes closed timer");
                eyesClosedStartTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() > eyesClosedStartTime + faceOutAlarmDelay && !setOffAlarm &&
                    (mediaPlayer == null || !mediaPlayer.isPlaying())) {
                Log.d(tag, "Starting alarm");

                // startAlarm exists to ensure another alarm doesn't go off after the first one ends
                setOffAlarm = true;
                tripActivity.startAlarm();
            }
        }

//        graphicOverlay.remove(eyesGraphic);
    }

    @Override
    /**
     * Removes the bounding boxes around the user's eyes when the FaceTracker is done being used
     */
    public void onDone() {
        graphicOverlay.remove(eyesGraphic);
    }

    /**
     * Updates the dictionary that contains each landmark's proportion
     * @param face Face object containing the positions of the user's landmarks
     */
    private void updatePreviousProportions(Face face) {
        for (Landmark landmark : face.getLandmarks()) {
            PointF position = landmark.getPosition();
            float xProp = (position.x - face.getPosition().x) / face.getWidth();
            float yProp = (position.y - face.getPosition().y) / face.getHeight();
            previousProportions.put(landmark.getType(), new PointF(xProp, yProp));
        }
    }

    /**
     * If the landmark exists on the face, return its position. If not, approximate it based on previous measurements
     * stored in the previousProportions HashMap
     *
     * @param face Face object containing the user's landmarks
     * @param landmarkId refers to which specific landmark is needed
     * @return a tuple representing the position of the specified landmark
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

