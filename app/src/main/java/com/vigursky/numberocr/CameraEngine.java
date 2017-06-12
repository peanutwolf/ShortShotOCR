package com.vigursky.numberocr;



import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

/**
 * Created by vigursky on 23.12.2016.
 */

public class CameraEngine {

    static final String TAG = "DBG_" + CameraEngine.class.getName();

    boolean on;
    Camera camera;
    SurfaceHolder surfaceHolder;


    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {

        }
    };

    public boolean isOn() {
        return on;
    }

    private CameraEngine(SurfaceHolder surfaceHolder){
        this.surfaceHolder = surfaceHolder;
    }

    static public CameraEngine New(SurfaceHolder surfaceHolder){
        Log.d(TAG, "Creating camera engine");
        return  new CameraEngine(surfaceHolder);
    }

    public void requestFocus() {
        if (camera == null)
            return;

        if (isOn()) {
            camera.autoFocus(autoFocusCallback);
        }
    }

    @SuppressWarnings("deprecation")
    public void start() {

        Log.d(TAG, "Entered CameraEngine - start()");
        this.camera = Camera.open();

        if (this.camera == null)
            return;

        Log.d(TAG, "Got camera hardware");

        try {

            Camera.Parameters params = camera.getParameters();
            if (params.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
            params.setPreviewSize(previewSizes.get(previewSizes.size()-1).width, previewSizes.get(previewSizes.size()-1).height);
            params.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(params);

            this.camera.setPreviewDisplay(this.surfaceHolder);
            this.camera.setDisplayOrientation(90);//Portrait Camera
            this.camera.startPreview();

            on = true;

            Log.d(TAG, "CameraEngine preview started");

        } catch (IOException e) {
            Log.e(TAG, "Error in setPreviewDisplay");
        }
    }

    public void restart(){
        if (camera != null){
            camera.stopPreview();
            camera.startPreview();
        }
    }

    public void stop(){

        if(camera != null){
//            this.autoFocusEngine.stop();
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        on = false;

        Log.d(TAG, "CameraEngine Stopped");
    }

    public void takeShot(Camera.ShutterCallback shutterCallback,
                         Camera.PictureCallback rawPictureCallback,
                         Camera.PictureCallback jpegPictureCallback ){
        if(isOn()){
            camera.takePicture(shutterCallback, rawPictureCallback, jpegPictureCallback);
        }
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback){
        camera.setPreviewCallback(previewCallback);
    }

}