package com.vigursky.numberocr;

import android.graphics.Bitmap;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vigursky on 10.01.2017.
 */

public class OCREvent {
    public static final int EVENT_REQUEST_PHONE_PERM = 0;
    public static final int EVENT_ID_LINES_DETECTED = 1;

    public  String mMessage;
    public  int mEventID;
    public Bitmap mOCRBitmap;
    private List<OCRPositionalFilter.DetectedLine> mDetectedLines;

    public static class Builder{
        private OCREvent mOCREvent = new OCREvent();

        public Builder setEventID(int eventID){
            mOCREvent.mEventID = eventID;
            return this;
        }

        public Builder addDetectedLines(List<OCRPositionalFilter.DetectedLine> linesList){
            mOCREvent.mDetectedLines.addAll(linesList);
            return this;
        }

        public Builder setDetectedBitmap(Bitmap detectedBitmap){
            mOCREvent.mOCRBitmap = detectedBitmap;
            return this;
        }

        public OCREvent build(){
            return mOCREvent;
        }

    }

    public List<OCRPositionalFilter.DetectedLine> getDetectedLines(){
        return mDetectedLines;
    }

    public Bitmap getDetectedBitmap(){
        return mOCRBitmap;
    }

    private OCREvent(){
        mDetectedLines = new ArrayList<>();
    }

    public OCREvent(int eventID, String message) {
        this.mMessage = message;
        this.mEventID = eventID;
    }

    public OCREvent(int eventID, String message, Bitmap OCRBitmap) {
        this(eventID, message);
        this.mOCRBitmap = OCRBitmap;
    }



}
