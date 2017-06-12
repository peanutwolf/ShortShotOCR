package com.vigursky.numberocr;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vigursky on 12.01.2017.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    public static final String TAG = OcrDetectorProcessor.class.toString();
    private OCRPositionalFilter mPositionalFilter = new OCRPositionalFilter();
    private List<OCRPositionalFilter.DetectedLine> mFilteringResult = new ArrayList<>();
    private IBitmapCallback mBitmapCallback;
    private boolean mProcessPaused;


    public OcrDetectorProcessor(IBitmapCallback bitmapCallback) {
        mBitmapCallback = bitmapCallback;
    }

    @Override
    public void release() {

    }

    public void reset(){
        mPositionalFilter.reset();
    }

    public synchronized void process_pause(boolean pause){
        mProcessPaused = pause;
    }

    @Override
    public synchronized void receiveDetections(Detector.Detections<TextBlock> detections) {
        SparseArray<TextBlock> items = detections.getDetectedItems();
        StringBuilder filteredDetectionsBuilder = new StringBuilder();
        StringBuilder rawDetectionsBuilder = new StringBuilder();
        if(items.size() == 0)
            return;
        Log.d(TAG, "========Detected [" + items.size() + "] blocks========");
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                mPositionalFilter.addDetectedTextToProcess(item);
                rawDetectionsBuilder.append(item.getValue());
            }
        }
        mFilteringResult.clear();
        filteredDetectionsBuilder.append(mPositionalFilter.processDetection(mFilteringResult));

        String rawDetection = rawDetectionsBuilder.toString();
        String filteredDetection = filteredDetectionsBuilder.toString();
        if(filteredDetection.isEmpty())
            return;
        Log.d("OcrDetectorProcessor", "Text detected: " + rawDetection);
        Log.d("OcrDetectorProcessor", "Text after filtering: " + filteredDetection);
        if(!mProcessPaused) {
            Bitmap detectedBitmap = null;
            if(mBitmapCallback != null){
                detectedBitmap = mBitmapCallback.getDetectedBitmap();
            }
            EventBus.getDefault().post(new OCREvent.Builder()
                    .setEventID(OCREvent.EVENT_ID_LINES_DETECTED)
                    .addDetectedLines(mFilteringResult)
                    .setDetectedBitmap(detectedBitmap)
                    .build());
        }
    }

    private class LevenshteinFilter{

        public double similarity(String s1, String s2) {

            String longer = s1, shorter = s2;
            if (s1.length() < s2.length()) {
                longer = s2; shorter = s1;
            }
            int longerLength = longer.length();
            if (longerLength == 0) {
                return 1.0;
            }
            return (longerLength - editDistance(longer, shorter)) / (double) longerLength;

        }

        // Example implementation of the Levenshtein Edit Distance
        // See http://r...content-available-to-author-only...e.org/wiki/Levenshtein_distance#Java
        public int editDistance(String s1, String s2) {
            s1 = s1.toLowerCase();
            s2 = s2.toLowerCase();

            int[] costs = new int[s2.length() + 1];
            for (int i = 0; i <= s1.length(); i++) {
                int lastValue = i;
                for (int j = 0; j <= s2.length(); j++) {
                    if (i == 0)
                        costs[j] = j;
                    else {
                        if (j > 0) {
                            int newValue = costs[j - 1];
                            if (s1.charAt(i - 1) != s2.charAt(j - 1))
                                newValue = Math.min(Math.min(newValue, lastValue),
                                        costs[j]) + 1;
                            costs[j - 1] = lastValue;
                            lastValue = newValue;
                        }
                    }
                }
                if (i > 0)
                    costs[s2.length()] = lastValue;
            }
            return costs[s2.length()];
        }

    }

    interface IBitmapCallback{
        Bitmap getDetectedBitmap();
    }
}
