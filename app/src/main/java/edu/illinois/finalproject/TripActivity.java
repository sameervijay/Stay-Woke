package edu.illinois.finalproject;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TripActivity extends AppCompatActivity {
    private String tag = "TripActivity";

    public Bitmap imageBitmap;
    public String photoFilePath;
    private ImageView imageView;

    public Uri photoUri;

    private Timer imageTimer;

    private Size previewSize;
    private Size[] jpegSizes = null;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;
    Button takePictureButton;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private FaceDetector detector;

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            startCamera();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {}
        @Override
        public void onError(CameraDevice camera, int error) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // To remove the title bar
        setTheme(R.style.AppTheme_NoTitleBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        // Following Camera permission request derived from:
        // https://stackoverflow.com/questions/35451833/requesting-camera-permission-with-android-sdk-23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }
        }

        textureView = (TextureView) findViewById(R.id.mainTextureView);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        imageView = (ImageView) findViewById(R.id.mainImageView);

        imageTimer = new Timer();
        imageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getPicture();
            }
        }, 0, 1000);
    }

    public void getPicture() {
        Log.d(tag, "Starting get picture");
        if (cameraDevice == null) {
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640, height = 480;
            for (Size size : jpegSizes) {
                Log.d("MainActivity", Integer.toString(size.getWidth()) + ", " + Integer.toString(size.getHeight()));
            }
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
            ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] imageByteArray = new byte[buffer.capacity()];
                        buffer.get(imageByteArray);
                        save(imageByteArray);
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
                    File outputFile = getOutputMediaFile();
                    OutputStream outputStream = null;

                    try {
                        outputStream = new FileOutputStream(outputFile);
                        outputStream.write(bytes);

                        analyzeImage(outputFile);
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

    public void openCamera() {
        Log.d(tag, "Opening camera");

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
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
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraDevice != null) {
            cameraDevice.close();
        }
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

    public void analyzeImage(File imageFile) {
        Log.d(tag, "Analyzing image");
        photoUri = Uri.fromFile(imageFile);

        try {
            photoFilePath = imageFile.getAbsolutePath();
            // Gets bitmap from the filepath and displays it in the image view
            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            imageView.setImageBitmap(imageBitmap);

            getFeaturesFromMobileVision(imageBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getFeaturesFromMobileVision(Bitmap imageBitmap) {
        Log.d("Google Vision", "Starting Google Mobile Vision");
        Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
        SparseArray<Face> faces = detector.detect(frame);
        Log.d("Google Vision", "Found " + faces.size() + " faces");

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            Log.d("Google Vision", "Left Open: " + Float.toString(face.getIsLeftEyeOpenProbability()));
            Log.d("Google Vision", "Right Open: " + Float.toString(face.getIsRightEyeOpenProbability()));
            Log.d("Google Vision", "Smiling: " + Float.toString(face.getIsSmilingProbability()));

//            for (Landmark landmark : face.getLandmarks()) {
//                Log.d("MainActivity", Integer.toString(landmark.getType()));
//                int cx = (int) (landmark.getPosition().x * scale);
//                int cy = (int) (landmark.getPosition().y * scale);
//                main.drawCircle(cx, cy, 10, paint);
//            }
        }

    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),"StayWoke");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MainActivity", "Failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }
}
