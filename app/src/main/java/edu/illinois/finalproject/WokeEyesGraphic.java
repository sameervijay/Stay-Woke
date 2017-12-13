package edu.illinois.finalproject;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import edu.illinois.finalproject.camera.GraphicOverlay;

/**
 * Created by sameervijay on 12/9/17.
 */

// Class derived from: Google Vision GooglyEyes Sample
// https://github.com/googlesamples/android-vision/blob/master/visionSamples/googly-eyes

public class WokeEyesGraphic extends GraphicOverlay.Graphic {
    private static final float EYE_RADIUS_PROPORTION = 0.45f;
    private Paint eyePaint;

    private volatile PointF leftPosition;
    private volatile PointF rightPosition;
    private volatile boolean leftOpen;
    private volatile boolean rightOpen;

    public WokeEyesGraphic(GraphicOverlay overlay) {
        super(overlay);

        eyePaint = new Paint();
        eyePaint.setColor(Color.WHITE);
        eyePaint.setStrokeWidth(7);
        eyePaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * Updates the instance variables with new left/right eye positions represented as tuples, and booleans
     * representing whether the left and right eyes are open
     * @param leftPosition PointF of the left eye's position
     * @param rightPosition PointF of the right eye's position
     * @param leftOpen boolean representing whether the left eye is open
     * @param rightOpen boolean representing whether the right eye is open
     */
    void updateEyes(PointF leftPosition, PointF rightPosition, boolean leftOpen, boolean rightOpen) {
        this.leftPosition = leftPosition;
        this.leftOpen = leftOpen;

        this.rightPosition = rightPosition;
        this.rightOpen = rightOpen;

        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        PointF detectLeftPosition = leftPosition;
        PointF detectRightPosition = rightPosition;
        if ((detectLeftPosition == null) || (detectRightPosition == null)) {
            return;
        }

        PointF leftPosition = new PointF(translateX(detectLeftPosition.x), translateY(detectLeftPosition.y));
        PointF rightPosition = new PointF(translateX(detectRightPosition.x), translateY(detectRightPosition.y));

        // Derives the approximate size of the eyes for the bounding boxes based off the inter-eye distance and
        // which is calculated with Pythagorean theorem
        float distance = (float) Math.sqrt(
                Math.pow(rightPosition.x - leftPosition.x, 2) + Math.pow(rightPosition.y - leftPosition.y, 2));
        float eyeRadius = EYE_RADIUS_PROPORTION * distance;

        // Draws both eyes given positions and whetherthey're open
        drawEye(canvas, leftPosition, eyeRadius, leftOpen);
        drawEye(canvas, rightPosition, eyeRadius, rightOpen);
    }

    /**
     * Actually draws the bounding box around the eye on the canvas. Called once for each eye
     *
     * @param canvas Canvas on which to draw the bounding box
     * @param eyePosition position of the bounding box
     * @param eyeRadius approximate size of the eye, which is used for the bounding box dimensions
     * @param isOpen boolean representing whether the eye is open
     */
    private void drawEye(Canvas canvas, PointF eyePosition, float eyeRadius, boolean isOpen) {
        Rect eyeBoundingBox = new Rect((int)(eyePosition.x - eyeRadius), (int)(eyePosition.y - eyeRadius),
                (int)(eyePosition.x + eyeRadius), (int)(eyePosition.y + eyeRadius));

        // Bounding box is green if the eye is open and red if it isn't
        if (isOpen) {
            eyePaint.setColor(Color.GREEN);
        } else {
            eyePaint.setColor(Color.RED);
        }

        // Draws the bounding box
        canvas.drawRect(eyeBoundingBox, eyePaint);
    }
}
