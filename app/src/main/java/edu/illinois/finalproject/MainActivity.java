package edu.illinois.finalproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Camera camera;
    private ImageHandler imageHandler;

    private Button startTripButton;

    private String logTag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        camera = new Camera(this);
//        imageHandler = new ImageHandler(this);

        startTripButton = (Button)findViewById(R.id.startTripButton);
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (camera.cameraDevice != null) {
//            camera.cameraDevice.close();
//        }
//    }

    public void onStartTripClicked(View view) {
        Log.d(logTag, "Start trip was pressed");
    }
}
