package edu.illinois.finalproject;

import com.google.android.gms.vision.face.Landmark;

import java.util.ArrayList;

/**
 * Created by sameervijay on 12/8/17.
 */

public class FaceData {
    public static float leftEyeOpenThreshold = 0.0f;
    public static float rightEyeOpenThreshold = 0.0f;

    private float leftEyeOpenProb = 0.0f;
    private float rightEyeOpenProb = 0.0f;

    private ArrayList<Landmark> landmarks = new ArrayList<>();

    private float eulerY = 0.0f;
    private float eulerZ = 0.0f;

    public FaceData() {
    }

    public FaceData(float leftEyeOpenProb, float rightEyeOpenProb) {
        this.leftEyeOpenProb = leftEyeOpenProb;
        this.rightEyeOpenProb = rightEyeOpenProb;
    }

    public float getLeftEyeOpenProb() {
        return leftEyeOpenProb;
    }

    public void setLeftEyeOpenProb(float leftEyeOpenProb) {
        this.leftEyeOpenProb = leftEyeOpenProb;
    }

    public float getRightEyeOpenProb() {
        return rightEyeOpenProb;
    }

    public void setRightEyeOpenProb(float rightEyeOpenProb) {
        this.rightEyeOpenProb = rightEyeOpenProb;
    }

    public ArrayList<Landmark> getLandmarks() {
        return landmarks;
    }

    public void setLandmarks(ArrayList<Landmark> landmarks) {
        this.landmarks = landmarks;
    }

    public float getEulerY() {
        return eulerY;
    }

    public void setEulerY(float eulerY) {
        this.eulerY = eulerY;
    }

    public float getEulerZ() {
        return eulerZ;
    }

    public void setEulerZ(float eulerZ) {
        this.eulerZ = eulerZ;
    }
}
