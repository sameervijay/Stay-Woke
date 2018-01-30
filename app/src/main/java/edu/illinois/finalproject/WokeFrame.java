package edu.illinois.finalproject;

/**
 * Created by sameervijay on 1/27/18.
 */

public class WokeFrame {
    private long timeStamp;
    private boolean faceDetected;
    private float leftOpenProb, rightOpenProb;

    public WokeFrame() {
    }

    public WokeFrame(long timeStamp, boolean faceDetected, float leftOpenProb, float rightOpenProb) {
        this.timeStamp = timeStamp;
        this.faceDetected = faceDetected;
        this.leftOpenProb = leftOpenProb;
        this.rightOpenProb = rightOpenProb;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isFaceDetected() {
        return faceDetected;
    }

    public void setFaceDetected(boolean faceDetected) {
        this.faceDetected = faceDetected;
    }

    public float getLeftOpenProb() {
        return leftOpenProb;
    }

    public void setLeftOpenProb(float leftOpenProb) {
        this.leftOpenProb = leftOpenProb;
    }

    public float getRightOpenProb() {
        return rightOpenProb;
    }

    public void setRightOpenProb(float rightOpenProb) {
        this.rightOpenProb = rightOpenProb;
    }

}
