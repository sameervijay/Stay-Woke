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

// Following class derived from:
// https://github.com/googlesamples/android-vision/blob/master/visionSamples/googly-eyes/app/src/main/java/com/google/
// android/gms/samples/vision/face/googlyeyes/GooglyEyesGraphic.java

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
        eyePaint.setStrokeWidth(10);
        eyePaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * Updates the eye positions and state from the detection of the most recent frame.  Invalidates
     * the relevant portions of the overlay to trigger a redraw.
     */
    void updateEyes(PointF leftPosition, boolean leftOpen,
                    PointF rightPosition, boolean rightOpen) {
        this.leftPosition = leftPosition;
        this.leftOpen = leftOpen;

        this.rightPosition = rightPosition;
        this.rightOpen = rightOpen;

        postInvalidate();
    }

    /**
     * Draws the current eye state to the supplied canvas.  This will draw the eyes at the last
     * reported position from the tracker, and the iris positions according to the physics
     * simulations for each iris given motion and other forces.
     */
    @Override
    public void draw(Canvas canvas) {
        PointF detectLeftPosition = leftPosition;
        PointF detectRightPosition = rightPosition;
        if ((detectLeftPosition == null) || (detectRightPosition == null)) {
            return;
        }

        PointF leftPosition = new PointF(translateX(detectLeftPosition.x), translateY(detectLeftPosition.y));
        PointF rightPosition = new PointF(translateX(detectRightPosition.x), translateY(detectRightPosition.y));

        // Use the inter-eye distance to set the size of the eyes.
        float distance = (float) Math.sqrt(
                Math.pow(rightPosition.x - leftPosition.x, 2) + Math.pow(rightPosition.y - leftPosition.y, 2));
        float eyeRadius = EYE_RADIUS_PROPORTION * distance;

        drawEye(canvas, leftPosition, eyeRadius, leftOpen);
        drawEye(canvas, rightPosition, eyeRadius, rightOpen);
    }

    private void drawEye(Canvas canvas, PointF eyePosition, float eyeRadius, boolean isOpen) {
        Rect leftRect = new Rect((int)(eyePosition.x - eyeRadius), (int)(eyePosition.y - eyeRadius),
                (int)(eyePosition.x + eyeRadius), (int)(eyePosition.y + eyeRadius));

        canvas.drawRect(leftRect, eyePaint);

        if (isOpen) {
            // Might change the color of the bounding box depending on whether the eye is open
        } else {
        }
    }
}
