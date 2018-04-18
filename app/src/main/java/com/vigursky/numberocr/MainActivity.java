package com.vigursky.numberocr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.crash.FirebaseCrash;
import com.vigursky.numberocr.camera.CameraSource;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener,
         ServiceConnection, OCRDialogFragment.ICallback {

    static final String TAG = "DBG_" + MainActivity.class.getName();
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    public static final int RC_HANDLE_CALL_PHONE_PERM = 3;
    private static final int RC_HANDLE_GMS = 9001;

    private boolean mInstantPhoneDetection = false;
    private boolean mInstantURLDetection = false;

    Animation mInProgressAnimation;
    TextView  mInProgressText;
    ImageView shutterButton;
    ImageView restartButton;
    ImageView settingsButton;
    ImageView historyButton;
    FocusBoxView focusBox;
    SurfaceView cameraFrame;
    SurfaceHolder mSurfaceHolder;
    OCRGraphicView mOCRGraphicView;
    CameraSource mCameraSource;
    TessService  mTessService;
    final Object mLock = new Object();
    List<OCRPositionalFilter.DetectedLine> mOCRDetectedLines = null;
    Bitmap mDetectedBitmap;
    SharedPreferences mSharedPref = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mSharedPref = getSharedPreferences(SettingsActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(true, false);
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);

    }

    private void requestCallPhonePermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CALL_PHONE};

        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CALL_PHONE_PERM);
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM && requestCode != RC_HANDLE_CALL_PHONE_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if(requestCode == RC_HANDLE_CAMERA_PERM){
                Log.d(TAG, "Camera permission granted - initialize the camera source");
                createCameraSource(true, false);
                if(mSurfaceHolder != null)
                    startCameraSource(mSurfaceHolder);
            }else if( requestCode == RC_HANDLE_CALL_PHONE_PERM){
                Log.d(TAG, "CallPhone permission granted");
            }

            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(requestCode == RC_HANDLE_CAMERA_PERM) {
                    finish();
                }
            }
        };

        String message = "Denied";
        if(requestCode == RC_HANDLE_CAMERA_PERM){
            message = getString(R.string.no_camera_permission);
        }else if( requestCode == RC_HANDLE_CALL_PHONE_PERM){
            message = getString(R.string.no_phone_permission);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permissions)
                .setMessage(message)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource(SurfaceHolder surfaceHolder) throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }
        if (mCameraSource != null) {
            try {
                mCameraSource.start(surfaceHolder);
                focusBox.setPreviewSize(mCameraSource.getPreviewSize().getWidth(), mCameraSource.getPreviewSize().getHeight());
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }else{
            Log.e(TAG, "Failed to start Camera. It is not created yet.");
        }
    }

    private void stopCameraSource(){
        if (mCameraSource != null) {
            mCameraSource.stop();
        }else{
            Log.e(TAG, "Failed to stop Camera. It was not created.");
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        mSurfaceHolder = holder;
        this.startCameraSource(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        this.stopCameraSource();
        holder.removeCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        Intent intent = new Intent(this, TessService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        mInProgressText = (TextView) findViewById(R.id.detect_progress_txt);
        cameraFrame     = (SurfaceView) findViewById(R.id.camera_frame);
        shutterButton   = (ImageView) findViewById(R.id.shutter_button);
        restartButton   = (ImageView) findViewById(R.id.restart_button);
        settingsButton  = (ImageView) findViewById(R.id.settings_button);
        historyButton   = (ImageView) findViewById(R.id.history_button);
        focusBox        = (FocusBoxView) findViewById(R.id.focus_box);
        mOCRGraphicView = (OCRGraphicView) findViewById(R.id.ocr_graphic_view);
        mOCRGraphicView.setFocusBox(focusBox);

        shutterButton.setOnClickListener(this);
        restartButton.setOnClickListener(this);
        settingsButton.setOnClickListener(this);
        historyButton.setOnClickListener(this);
        SurfaceHolder surfaceHolder = cameraFrame.getHolder();
        surfaceHolder.addCallback(this);

        mInstantPhoneDetection = mSharedPref.getBoolean(SettingsActivity.TAG_PHONE_SWITCH_VALUE, true);
        mInstantURLDetection   = mSharedPref.getBoolean(SettingsActivity.TAG_URL_SWITCH_VALUE, true);

        mInProgressAnimation = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.processing_text);

        EventBus.getDefault().register(this);
    }



    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        unbindService(this);
        EventBus.getDefault().unregister(this);
    }

    private void displayDetectionDialog(String detectedValue){

        if(detectedValue == null)
            return;
        pause_detection(true);
        ArrayList<CharSequence> listItems = new ArrayList<>();
        listItems.add(detectedValue);
        DialogFragment mOCRDialogFragment = new OCRDialogFragment();
        Bundle arg = new Bundle();
        arg.putCharSequenceArrayList(OCRDialogFragment.OCR_TEXT_TAG, listItems);
        mOCRDialogFragment.setArguments(arg);
        mOCRDialogFragment.show(getFragmentManager(), "OCR_action");
    }

    private void onShutterButtonClicked(){
        ArrayList<CharSequence> listItems = new ArrayList<>();
        DialogFragment mOCRDialogFragment;

        if (getFragmentManager().findFragmentByTag("OCR_action") != null) return;

        pause_detection(true);
        synchronized (mLock){
            if(mOCRDetectedLines != null) {
                for (OCRPositionalFilter.DetectedLine mLines : mOCRDetectedLines) {
                    listItems.add(mLines.getLineValue());
                }
            }
        }
        if(listItems.size() == 0){
            mOCRDialogFragment = new NoDetectionsDialog();
        }else{
            mOCRDialogFragment = new OCRDialogFragment();
            Bundle arg = new Bundle();
            arg.putCharSequenceArrayList(OCRDialogFragment.OCR_TEXT_TAG, listItems);
            arg.putParcelable(OCRDialogFragment.OCR_BITMAP_TAG, mDetectedBitmap);
            mOCRDialogFragment.setArguments(arg);
        }
        mOCRDialogFragment.show(getFragmentManager(), "OCR_action");
    }

    private void resetDetectedLines(){
        if(mTessService != null)
            mTessService.reset();
        mOCRDetectedLines = null;
        mOCRGraphicView.drawGraphics(null);
    }

    private void onRestartButtonClicked(){
        resetDetectedLines();
        focusBox.resetBox();
    }

    private void onSettingsButtonClicked(){
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void onHistoryButtonClicked(){
        if (getFragmentManager().findFragmentByTag("OCR_history") != null) return;

        pause_detection(true);
        DialogFragment mOCRHistoryDialogFragment = new OCRHistoryDialogFragment();
        mOCRHistoryDialogFragment.show(getFragmentManager(), "OCR_history");
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.shutter_button){
            this.onShutterButtonClicked();
        }else if(v.getId() == R.id.restart_button){
            this.onRestartButtonClicked();
        }else if(v.getId() == R.id.settings_button) {
            this.onSettingsButtonClicked();
        }else if(v.getId() == R.id.history_button) {
            this.onHistoryButtonClicked();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "TessService:onServiceConnected");
        mTessService = ((TessService.TessBinder)service).getService();
        mTessService.setCameraSource(mCameraSource);
        mTessService.setFocusBox(focusBox);
        mTessService.process_data();
        mInProgressText.startAnimation(mInProgressAnimation);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "TessService:onServiceDisconnected");
        mTessService = null;
        pause_detection(true);
    }


    public void checkInstantDetections(@NonNull List<OCRPositionalFilter.DetectedLine> lines){
        for(OCRPositionalFilter.DetectedLine detectedLine : lines){
            String lineValue = detectedLine.getLineValue();
            int detectionCounter = detectedLine.mDetectionCounter;
            if(detectionCounter < 2)
                continue;
            if(mInstantPhoneDetection){
                if(Tools.isPhoneValidNumber(lineValue)){
                    onShutterButtonClicked();
                    return;
                }
            }
            if(mInstantURLDetection){
                if(Tools.isValidURL(lineValue)){
                    onShutterButtonClicked();
                    return;
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(OCREvent event) {
        switch (event.mEventID){
            case OCREvent.EVENT_REQUEST_PHONE_PERM:
                requestCallPhonePermission();
                break;
            case OCREvent.EVENT_ID_LINES_DETECTED:
                synchronized (mLock){
                    mOCRDetectedLines = event.getDetectedLines();
                    mDetectedBitmap = event.getDetectedBitmap();
                    mOCRGraphicView.drawGraphics(mOCRDetectedLines);
                    checkInstantDetections(mOCRDetectedLines);
                }
                break;
            default:
                break;
        }
    }

    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        mCameraSource =
                new CameraSource.Builder(getApplicationContext())
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(30.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .setFocusBox((FocusBoxView) findViewById(R.id.focus_box))
                        .build();
    }

    @Override
    public void OnDialogDismiss() {
        pause_detection(false);
        resetDetectedLines();
    }

    private void pause_detection(boolean pause){
        if(pause){
            mInProgressText.clearAnimation();
            mInProgressText.setVisibility(View.GONE);
        }else{
            mInProgressText.setVisibility(View.VISIBLE);
            mInProgressText.startAnimation(mInProgressAnimation);
        }
        if(mTessService != null)
            mTessService.process_pause(pause);
    }
}