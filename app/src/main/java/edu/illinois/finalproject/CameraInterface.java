package edu.illinois.finalproject;

import android.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sameervijay on 12/5/17.
 */

public class CameraInterface {
    private Size[] jpegSizes = null;
    private Size previewSize;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;

    private CameraDevice cameraDevice;
    private TripActivity tripActivity;
    private TextureView textureView;

    private String tag = "CameraInterface";

    public TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(tag, "Surface texture available");
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(tag, "State callback onOpened called");
            cameraDevice = camera;
            startCamera();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
        }

        @Override
        public void onError(CameraDevice camera, int error) {
        }
    };

    public CameraInterface(TripActivity activity, TextureView texture) {
        tripActivity = activity;
        textureView = texture;

        textureView.setSurfaceTextureListener(surfaceTextureListener);

        // Following Camera permission request derived from:
        // https://stackoverflow.com/questions/35451833/requesting-camera-permission-with-android-sdk-23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (tripActivity.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(tripActivity, new String[]{android.Manifest.permission.CAMERA}, 1);
            }
        }

        Log.d(tag, "Instantiating CameraInterface");
    }

    public void startCamera() {
        Log.d(tag, "Starting camera");
        if (cameraDevice == null || !textureView.isAvailable() || previewSize == null) {
            return;
        }
        SurfaceTexture texture = textureView.getSurfaceTexture();
        if (texture == null) {
            return;
        }
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = new Surface(texture);
        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (Exception e) {
            e.printStackTrace();
        }
        previewBuilder.addTarget(surface);
        try {
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    previewSession = session;
                    getChangedPreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getPicture() {
        Log.d(tag, "Starting get picture");
        if (cameraDevice == null) {
            Log.d(tag, "Camera device is null; aborting getPicture");
            return;
        }
        CameraManager manager = (CameraManager) tripActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640, height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            requestBuilder.addTarget(reader.getSurface());
            requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270);

            FaceData faceData = null;
            ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] imageByteArray = new byte[buffer.capacity()];
                        buffer.get(imageByteArray);

                        Log.d(tag, "Image available");

                        // Analyzes the image for faces and returns a pointer to a FaceData object that contains
                        // the relevant characteristics of the face
                        FaceData faceFound = tripActivity.getImageHandler().analyzeImage(imageByteArray);
                        tripActivity.processFace(faceFound);
//                        save(imageByteArray);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                public void save(byte[] bytes) {
                    // Gets the file to save the image to
                    File outputFile = TripActivity.getOutputMediaFile();
                    OutputStream outputStream = null;

                    try {
                        outputStream = new FileOutputStream(outputFile);
                        outputStream.write(bytes);

                        // Actually runs the computer vision on the image in the output file
                        Log.d(tag, "Saving image");
                        tripActivity.getImageHandler().analyzeImage(bytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (outputStream != null) {
                                outputStream.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            HandlerThread handlerThread = new HandlerThread("TakePicture");
            handlerThread.start();
            final Handler handler = new Handler(handlerThread.getLooper());
            reader.setOnImageAvailableListener(imageAvailableListener, handler);
            final CameraCaptureSession.CaptureCallback previewSession = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    startCamera();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(requestBuilder.build(), previewSession, handler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getChangedPreview() {
        if (cameraDevice == null) {
            return;
        }
        previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("Changed Preview");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        try {
            previewSession.setRepeatingRequest(previewBuilder.build(), null, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openCamera() {
        Log.d(tag, "Opening camera");

        CameraManager manager = (CameraManager) tripActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            // Defaults to back-facing camera generally; if there's a front camera, use it
            String cameraId = manager.getCameraIdList()[0];
            for (String camera : manager.getCameraIdList()) {
                CameraCharacteristics camCharacteristics = manager.getCameraCharacteristics(camera);
                if (camCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = camera;
                }
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(tripActivity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CameraDevice getCameraDevice() {
        return cameraDevice;
    }
}
