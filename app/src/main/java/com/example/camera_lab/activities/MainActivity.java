package com.example.camera_lab.activities;

import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;

import com.example.camera_lab.R;

public class MainActivity extends AppCompatActivity{

    static String TAG = "camera_lab";

    // views
    SurfaceView mSurfaceView;

    // camera
    CameraManager mCameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // binding views
        mSurfaceView = findViewById(R.id.surfaceView);

        // setting objects
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    void initialSetup() {

    }

}
