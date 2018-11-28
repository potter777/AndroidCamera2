package com.example.camera_lab.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
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
    // prefix "C" = current
    private CameraManager mCameraManager;
    private CameraSettings mSettings;
    private String mCCameraId;
    private CameraSide mCCameraSide;
    private boolean mIsFlashAvailable;
    private FlashOption mCFlashOption;
    private CameraDevice mCamera;
    private CameraCaptureSession mCameraSession;
    private String[] mCameras;
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    enum CameraSide {
        REAR,
        FRONTAL
    }

    enum FlashOption {
        AUTOMATIC,
        DISABLED,
        ENABLED
    }

    CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "camera is open");
            mCamera = camera;
            configureUXCurrentCamera(mCCameraSide);
            try {
                mCamera.createCaptureSession(
                        Collections.singletonList(mSurfaceView.getHolder().getSurface()),
                        mSessionStateCallback,
                        mHandler
                );
            } catch (CameraAccessException e) {
                Log.e(TAG, "" + e.getMessage());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i(TAG, "camera was closed");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "error: " + error);
        }
    };

    CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            try {
                Log.i(TAG, "session is configured");
                CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.addTarget(mSurfaceView.getHolder().getSurface());
                session.setRepeatingRequest(builder.build(), mSessionCaptureCallback, mHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "" + e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "session could not be configured");
        }
    };

    CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
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
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        /* Esto permite ejecutar los procesos en un hilo separado al de UI */
        mHandlerThread = new HandlerThread("camera_process");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        /* ***** ***** ***** */

        // binding views
        bindViews();
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
            mHandlerThread.quitSafely();
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
                    mHandler.post(() -> {
                       mCamera.close();
                       String cameraId;

                       if (mCCameraSide == CameraSide.FRONTAL) {
                           cameraId = getRearCamera();
                       }else{
                           cameraId = getFrontalCamera();
                       }

                       openCamera(cameraId);
                       mSettings.setCamera(cameraId);
                    });
                }
                break;
            case R.id.btnTakePhoto:
                break;
        }
    }

    void bindViews() {

        mSurfaceView = findViewById(R.id.surfaceView);
        mBtnFlash = findViewById(R.id.icFlash);
        mBtnCameraSide = findViewById(R.id.icCameraSide);
        mBtnTakePicture = findViewById(R.id.btnTakePhoto);
        mBtnFlash.setOnClickListener(this);
        mBtnCameraSide.setOnClickListener(this);
        mBtnTakePicture.setOnClickListener(this);

        // setting objects
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initialSetup();
                openCamera(mCCameraId);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });
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
                mCFlashOption = FlashOption.DISABLED;
                mSettings.setFlash(FlashOption.DISABLED.toString());
            } else {
                mCCameraId = mSettings.getCamera();
                mCFlashOption = FlashOption.valueOf(mSettings.getCamera());
            }

            if (mCCameraId.equals(getFrontalCamera())) {
                mCCameraSide = CameraSide.FRONTAL;
            }else{
                mCCameraSide = CameraSide.REAR;
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
        logCharacteristics(cameraId);
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
     * Evalua si es posible utilizar el flash
     * @param cameraId identificador de la camara
     * @return Verdadero cuando se pueda utilizar el flash
     */
    boolean isFlashAvailable (String cameraId) {

        if (mCameraManager == null) throw new IllegalStateException("Camera manager is not available");

        try {
            CameraCharacteristics ch = mCameraManager.getCameraCharacteristics(cameraId);
            Boolean isAvailable = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            return isAvailable != null && isAvailable;
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    /**
     *
     * @return id de la camara frontal
     */
    String getFrontalCamera() {
        mCCameraSide = CameraSide.FRONTAL;
        return selectCameraSide(CameraCharacteristics.LENS_FACING_FRONT);
    }

    /**
     * @return id de la camara trasera
     */
    String getRearCamera() {
        mCCameraSide = CameraSide.REAR;
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
        private static final String KEY_FLASH = "camera_flash";

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

        String getFlash() {
            // AUTOMATIC es parte del enum para la actividad de camara
            return mStore.getString(KEY_CAMERA, "AUTOMATIC");
        }

        void setFlash(String flashOption) {
            mStore.edit().putString(KEY_FLASH, flashOption).apply();
        }
    }

    void logCharacteristics(String cameraId) {
        try {
            CameraCharacteristics ch = mCameraManager.getCameraCharacteristics(cameraId);
            Log.i(TAG,"flash info available: " + ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));
            StreamConfigurationMap scm = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (scm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Size[] sizes = scm.getHighResolutionOutputSizes(ImageFormat.JPEG);
                    Log.i(TAG, "--------- High Resolution Output Sizes ---------");
                    for (Size size: sizes) {
                        Log.i(TAG, "Size: " + size.getWidth() + "w " + size.getHeight() + "h");
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Configura la interfaz de usuario según la camara que se haya seleccionado
     * @param cameraSide lado de la camara actual.
     */
    void configureUXCurrentCamera(CameraSide cameraSide) {
        runOnUiThread(() -> {
            Drawable drawable = null;
            switch (cameraSide) {
                case REAR:
                    drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_camera_front_24dp);
                    break;
                case FRONTAL:
                    drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_camera_rear_24dp);
                    break;
            }
            mBtnCameraSide.setImageDrawable(drawable);
        });
    }

}
