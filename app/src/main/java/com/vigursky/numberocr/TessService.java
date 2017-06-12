package com.vigursky.numberocr;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.vigursky.numberocr.camera.CameraSource;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by vigursky on 27.12.2016.
 */

public class TessService extends Service {

    public static final String TAG = TessService.class.getSimpleName();

    private final Object mLock = new Object();
    private final IBinder mBinder = new TessBinder();
    private Looper mServiceLooper;
    private GoogleVisionHandler mServiceHandler;

    private CameraSource mCameraSource;
    private TextRecognizer mTextRecognizer;
    private OcrDetectorProcessor mOcrDetectorProcessor;
    private FocusBoxView mFocusBox;

    private boolean isRunning = true;

    private class SimulateVisionHandler extends GoogleVisionHandler{

        public SimulateVisionHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            OCRPositionalFilter filter = new OCRPositionalFilter();
            List<OCRPositionalFilter.DetectedLine> linesList = new ArrayList<>();
            for(int i = 0; i < 10; i++){
                linesList.add(filter.new DetectedLine(i + ""));
            }
            linesList.add(filter.new DetectedLine("89210988095"));
            linesList.add(filter.new DetectedLine("www.peanutwolf.com"));

            while (isRunning){
                EventBus.getDefault().post(new OCREvent.Builder()
                        .setEventID(OCREvent.EVENT_ID_LINES_DETECTED)
                        .addDetectedLines(linesList)
                        .build());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private class GoogleVisionHandler extends Handler implements OcrDetectorProcessor.IBitmapCallback{

        PreviewArrayOutputStream out = new PreviewArrayOutputStream();
        int mPreviewWidth = 0;
        int mPreviewHeight = 0;
        int mPreviewFormat = ImageFormat.NV21;
        Bitmap mCurrentPreviewBitmap = null;

        public GoogleVisionHandler(Looper looper) {
            super(looper);
        }

        private Bitmap getPreviewBitmap(byte[] data){

            out.reset();

            YuvImage yuv = new YuvImage(data,mPreviewFormat, mPreviewWidth, mPreviewHeight, null);
            Rect scaledRect = mFocusBox.getPreviewScaledBox(mCameraSource.getRotation());
            yuv.compressToJpeg(scaledRect, 100, out);

            byte[] bytes = out.getBytes();

            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

        @Override
        public void handleMessage(Message msg) {
            CameraSource.PendingFrame pendingFrame;
            Frame outputFrame;

            if(mCameraSource == null){
                Log.d(TAG, "Handler not started due to CameraSource not set");
                return;
            }

            mPreviewWidth = mCameraSource.getPreviewSize().getWidth();
            mPreviewHeight = mCameraSource.getPreviewSize().getHeight();

            while(isRunning) {
                synchronized (mCameraSource) {
                    while (mCameraSource.isActive() && mCameraSource.getNextFrame().mPendingFrameData == null) {
                        try {
                            mCameraSource.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }

                    if(!mCameraSource.isActive())
                        return;

                    pendingFrame = mCameraSource.getNextFrame();

                    Bitmap bmp = getPreviewBitmap(pendingFrame.mPendingFrameData.array());
                    mCurrentPreviewBitmap = Tools.rotate(bmp, mCameraSource.getRotationAngle());

                    outputFrame = new Frame.Builder().setBitmap(bmp)
                            .setId(pendingFrame.mPendingFrameId)
                            .setTimestampMillis(pendingFrame.mPendingTimeMillis)
                            .setRotation(mCameraSource.getRotation())
                            .build();

                    pendingFrame.releaseFrameData();
                }

                synchronized (mLock){
                    try {
                        if(isRunning){
                            mTextRecognizer.receiveFrame(outputFrame);
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Exception thrown from receiver.", t);
                    } finally {
                        Bitmap outputBitmap = outputFrame.getBitmap();
                        if(outputBitmap != mCurrentPreviewBitmap)
                            outputBitmap.recycle();
                    }
                }
            }
        }

        @Override
        public Bitmap getDetectedBitmap() {
            return mCurrentPreviewBitmap;
        }
    }

    public class TessBinder extends Binder {
        public TessService getService(){
            return TessService.this;
        }

    }

    public void setCameraSource(CameraSource cameraSource){
        mCameraSource = cameraSource;
    }

    public void setFocusBox(FocusBoxView focusBox){
        mFocusBox = focusBox;
    }

    private void init_tess_thread(){
        HandlerThread thread = new HandlerThread("TessServiceThread");
        thread.start();

        mServiceLooper = thread.getLooper();
//        mServiceHandler = new GoogleVisionHandler(mServiceLooper);
        mServiceHandler = new SimulateVisionHandler(mServiceLooper);
    }

    public void reset(){
        synchronized (mLock){
            mOcrDetectorProcessor.reset();
        }
    }

    public void process_data(){
        Message msg = mServiceHandler.obtainMessage();
        msg.what = 0;
        mServiceHandler.sendMessage(msg);
    }

    public void process_pause(boolean pause){
        if(mCameraSource != null)
            mCameraSource.pauseCameraFraming(pause);
        mOcrDetectorProcessor.process_pause(pause);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        Context context = getApplicationContext();

        init_tess_thread();

        mOcrDetectorProcessor = new OcrDetectorProcessor(mServiceHandler);
        mTextRecognizer = new TextRecognizer.Builder(context).build();
        mTextRecognizer.setProcessor(mOcrDetectorProcessor);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        synchronized (mLock){
            isRunning = false;
            mTextRecognizer.release();
        }
        if(mServiceLooper != null)
            mServiceLooper.quit();
    }
}
