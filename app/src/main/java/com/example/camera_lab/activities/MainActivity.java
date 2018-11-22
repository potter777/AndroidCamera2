package com.example.camera_lab.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.camera_lab.R;

import java.util.Collections;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static String TAG = "camera_lab";

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    // views
    SurfaceView mSurfaceView;
    ImageButton mBtnFlash;
    ImageButton mBtnCameraSide;
    Button mBtnTakePicture;

    // camera
    CameraManager mCameraManager;
    CameraSettings mSettings;
    String mCCameraId;
    CameraDevice mCamera;
    CameraCaptureSession mCameraSession;
    String[] mCameras;
    Handler mHandler;

    CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            Log.i(TAG, "camera is open");
            try {
                mCamera.createCaptureSession(
                        Collections.singletonList(mSurfaceView.getHolder().getSurface()),
                        mCameraCaptureSessionCallback,
                        mHandler
                );
            } catch (CameraAccessException e) {
                Log.e(TAG, "" + e.getMessage());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "error: " + error);
        }
    };

    CameraCaptureSession.StateCallback mCameraCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            try {
                Log.i(TAG, "session is configured");
                CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.addTarget(mSurfaceView.getHolder().getSurface());
                session.setRepeatingRequest(builder.build(), mCameraCaptureCallback, mHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "" + e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "session could not be configured");
        }
    };

    CameraCaptureSession.CaptureCallback mCameraCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "capture failed");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // binding views
        bindViews();
        mSurfaceView = findViewById(R.id.surfaceView);

        // setting objects
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initialSetup();
                openCamera(mCCameraId);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCamera != null) {
            mCamera.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(mCCameraId);
            } else {
                disableViewInterface();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.icFlash:
                break;
            case R.id.icCameraSide:
                if (mCamera != null) {
                    mCamera.close();
                }
                break;
            case R.id.btnTakePhoto:
                break;
        }
    }

    void bindViews() {
        mBtnFlash = findViewById(R.id.icFlash);
        mBtnCameraSide = findViewById(R.id.icCameraSide);
        mBtnTakePicture = findViewById(R.id.btnTakePhoto);
        mBtnFlash.setOnClickListener(this);
        mBtnCameraSide.setOnClickListener(this);
        mBtnTakePicture.setOnClickListener(this);
    }

    /**
     * Evalua si se tiene permisos para utilizar la camara
     * @return Verdadero si existen los permisos
     */
    boolean hasCameraPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Solicita los permisos para utilizar la camara
     */
    void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    /**
     * Realiza los procesos necesarios para establecer la configuración previa del usuario
     */
    void initialSetup() {
        Log.i(TAG, "initialSetup()");
        try {

            mSettings = new CameraSettings(this);
            mCameras = mCameraManager.getCameraIdList();

            if (mSettings.isFirstTime()) {
                mCCameraId = getRearCamera();
                mSettings.setCamera(mCCameraId);
            } else {
                mCCameraId = mSettings.getCamera();
            }

        } catch (CameraAccessException e) {
            Log.e(TAG, "" + e.getMessage());
        }

        Log.i(TAG, "current camera: " + mCCameraId);
    }

    /**
     * Abre la camara seleccionada
     */
    @SuppressLint("MissingPermission")
    void openCamera(String cameraId) {
        Log.i(TAG, "openCamera()");

        if (!hasCameraPermission()) {
            requestCameraPermission();
            return;
        }

        try {
            mCameraManager.openCamera(cameraId, mCameraStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "" + e.getMessage());
        }
    }

    /**
     *
     * @param side camara frontal o trasera
     * @return id de la camara según el lado que se desea.
     */
    String selectCameraSide(int side) {

        if (mCameras == null || mCameras.length == 0) return null;

        for (String id : mCameras) {
            try {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer orientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (orientation != null && orientation == side) return id;
            } catch (CameraAccessException e) {
                Log.e(TAG, "" + e.getMessage());
                return null;
            }
        }

        return null;
    }

    /**
     *
     * @return id de la camara frontal
     */
    String getFrontalCamera() {
        return selectCameraSide(CameraCharacteristics.LENS_FACING_FRONT);
    }

    /**
     * @return id de la camara trasera
     */
    String getRearCamera() {
        return selectCameraSide(CameraCharacteristics.LENS_FACING_BACK);
    }

    /**
     * Cuando no se conceda permisos, los elementos visuales de la camara son ocultos
     */
    void disableViewInterface() {
        mBtnCameraSide.setEnabled(false);
        mBtnFlash.setEnabled(false);
        mBtnTakePicture.setEnabled(false);
    }

    /**
     * Gestiona la configuración de camara que el usuario utiliza
     */
    private static class CameraSettings {

        private static final String PREFERENCES = "camera_preferences";
        private static final String KEY_CAMERA = "camera_side";

        private Context mContext;

        private SharedPreferences mStore;

        CameraSettings(Context context) {
            mContext = context;
            mStore = mContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        }

        /**
         * Evalua si es la primera vez que se usa la camara
         * @return <code>true</code> si es la primera vez
         */
        boolean isFirstTime() {
            boolean isFirst = mStore.getBoolean("first_time", true);
            mStore.edit().putBoolean("first_time", false).apply();
            return isFirst;
        }

        String getCamera() {
            return mStore.getString(KEY_CAMERA, "");
        }

        void setCamera(String mCamera) {
            mStore.edit().putString(KEY_CAMERA, mCamera).apply();
        }
    }
}
