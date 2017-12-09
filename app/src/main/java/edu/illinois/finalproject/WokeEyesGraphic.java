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

    void updateEyes(PointF leftPosition, boolean leftOpen,
                    PointF rightPosition, boolean rightOpen) {
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

        // Use the inter-eye distance to set the size of the eyes.
        float distance = (float) Math.sqrt(
                Math.pow(rightPosition.x - leftPosition.x, 2) + Math.pow(rightPosition.y - leftPosition.y, 2));
        float eyeRadius = EYE_RADIUS_PROPORTION * distance;

        // Draws a single eye given position and whether it's open
        drawEye(canvas, leftPosition, eyeRadius, leftOpen);
        drawEye(canvas, rightPosition, eyeRadius, rightOpen);
    }

    private void drawEye(Canvas canvas, PointF eyePosition, float eyeRadius, boolean isOpen) {
        Rect leftRect = new Rect((int)(eyePosition.x - eyeRadius), (int)(eyePosition.y - eyeRadius),
                (int)(eyePosition.x + eyeRadius), (int)(eyePosition.y + eyeRadius));

        if (isOpen) {
            eyePaint.setColor(Color.GREEN);
        } else {
            eyePaint.setColor(Color.RED);
        }
        canvas.drawRect(leftRect, eyePaint);
    }
}
