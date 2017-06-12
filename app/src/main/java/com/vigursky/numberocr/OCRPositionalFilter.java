package com.vigursky.numberocr;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by vigursky on 26.01.2017.
 */

public class OCRPositionalFilter {
    public static final String TAG = OCRPositionalFilter.class.getSimpleName();
    private static final int PRIORITY_COUNTER_MAX_VALUE = 7;
    private static int mIdCounter = 0;
    private List<DetectedLine> mDetectionLines = new CopyOnWriteArrayList<>();
    private SortedSet<DetectedParcel> mFormedParcels = new TreeSet<>(new PositionalComparator());
    private StringBuilder mProcessDetectonResult = new StringBuilder();
    private int mVerticalThreshold = 20;

    private abstract class ParcelPosition{
        Rect mBoundingBox;

        ParcelPosition(Rect boundingBox){
            mBoundingBox = boundingBox;
        }

        /**
         * Returns vertical position of boundingBox
         *
         * @param   parcelPosition   an argument.
         * @return  0 if inline
         *          1 if detectedText is higher
         *          -1 of detectedText is lower
         */
        int compareVertical(ParcelPosition parcelPosition){
            Rect boundingBox = parcelPosition.mBoundingBox;
            int top    = boundingBox.top;
            int bottom = boundingBox.bottom;

            int topThreshold = Math.abs(top - mBoundingBox.top);
            int botThreshold = Math.abs(bottom - mBoundingBox.bottom);

            if(topThreshold < mVerticalThreshold && botThreshold < mVerticalThreshold){
                return 0;
            }else if(top > mBoundingBox.top || bottom > mBoundingBox.bottom){
                return 1;
            }else{
                return -1;
            }
        }

        /**
         * Returns horizontal position of boundingBox
         *
         * @param   parcelPosition   an argument.
         * @return  0 if inline
         *          1 if detectedText is right
         *          -1 of detectedText is left
         */
        int compareHorizontal(ParcelPosition parcelPosition){
            Rect boundingBox = parcelPosition.mBoundingBox;
            int left    = boundingBox.left;
            int right = boundingBox.right;

            if(mBoundingBox.left == left && mBoundingBox.right == right){
                return 0;
            }else if(left > mBoundingBox.left && right > mBoundingBox.right){
                return 1;
            }else{
                return -1;
            }
        }

        boolean isInline(ParcelPosition parcelPosition){
            Rect boundingBox = parcelPosition.mBoundingBox;
            int top    = boundingBox.top;
            int bottom = boundingBox.bottom;

            int topThreshold = Math.abs(mBoundingBox.top - top);
            int bottomThreshold = Math.abs(mBoundingBox.bottom - bottom);
            if(topThreshold <= mVerticalThreshold && bottomThreshold <= mVerticalThreshold){
                return true;
            }

            return false;
        }

    }

    private class DetectedParcel extends ParcelPosition{
        char   mCharValue;
        boolean mMerged = false;
        int mPriority;

        DetectedParcel(Rect boundingBox, char charValue) {
            super(boundingBox);
            mCharValue = charValue;
            mPriority = PRIORITY_COUNTER_MAX_VALUE;
        }

        @Override
        public String toString() {
            return "DetectedParcel: mCharValue=" + mCharValue
                    + " mBoundingBox=" + mBoundingBox.toShortString()
                    + " mPriority" + mPriority;
        }
    }

    public class DetectedLine{
        int mId;
        int mDetectionCounter = 1;
        String mLineValue;
        boolean mMerged = false;
        Rect   mBoundingBox = new Rect(-1,-1,-1,-1);
        SortedSet<DetectedParcel> mParcels = new TreeSet<>(new PositionalComparator());

        public DetectedLine(String lineValue){
            mLineValue = lineValue;
        }

        private DetectedLine(List<DetectedParcel> detectedParcels){
            mId = mIdCounter++;
            mParcels.addAll(detectedParcels);
            mLineValue = mergeInlineParcels(mParcels);
        }

        private String mergeInlineParcels(@NonNull SortedSet<DetectedParcel> inlineParcels){
            StringBuilder textValue = new StringBuilder();

            for (DetectedParcel parcel : inlineParcels) {
                Rect boundingBox_tmp = parcel.mBoundingBox;
                int top = Math.max(mBoundingBox.top, boundingBox_tmp.top);
                int bottom = Math.max(mBoundingBox.bottom, boundingBox_tmp.bottom);
                int left = mBoundingBox.left == -1 ? boundingBox_tmp.left : Math.min(mBoundingBox.left, boundingBox_tmp.left);
                int right = Math.max(mBoundingBox.right, boundingBox_tmp.right);
                if (mBoundingBox.right != -1 && (left - mBoundingBox.right) > (mBoundingBox.right - mBoundingBox.left)) {
                    textValue.append(" ");
                }
                textValue.append(parcel.mCharValue);
                mBoundingBox.set(left, top, right, bottom);
            }

            return textValue.toString();
        }

        private void update(DetectedLine detectedLine, int offset) {
            int i = 0;

            mDetectionCounter++;
            Log.d(TAG, "increasePriority mDetectionCounter=" + mDetectionCounter);

            for(DetectedParcel parcel : mParcels){
                if(i++ >= offset){
                    parcel.mPriority = PRIORITY_COUNTER_MAX_VALUE;
                }
                parcel.mBoundingBox.top = detectedLine.mBoundingBox.top;
                parcel.mBoundingBox.bottom = detectedLine.mBoundingBox.bottom;
            }

            mBoundingBox.top = detectedLine.mBoundingBox.top;
            mBoundingBox.bottom = detectedLine.mBoundingBox.bottom;
        }

        private boolean update(){
            Iterator<DetectedParcel> parcelsIt = mParcels.iterator();

            while (parcelsIt.hasNext()){
                DetectedParcel parcel = parcelsIt.next();
                if(--parcel.mPriority == 0){
                    parcelsIt.remove();
                }
            }

            mLineValue = mergeInlineParcels(mParcels);

            return mParcels.isEmpty();
        }

        @Override
        public String toString() {
            return "DetectedLine: mLineValue=" + mLineValue
                    + " mBoundingBox=" + mBoundingBox.toShortString()
                    + " mDetectionCounter=" + mDetectionCounter;
        }

        public Rect getBoundingBox(){
            return mBoundingBox;
        }

        public String getLineValue(){
            return mLineValue;
        }

    }

    public void addDetectedTextToProcess(@NonNull TextBlock textBlock){

        Log.d(TAG, "formTextBlockParcels content start>>>>>");
        formTextBlockParcels(mFormedParcels, textBlock);
        Log.d(TAG, "formTextBlockParcels content end<<<<<<<");

    }

    public String processDetection(@NonNull List<DetectedLine> filterResult, @NonNull TextBlock textBlock){

        addDetectedTextToProcess(textBlock);

        return processDetection(filterResult);
    }

    public void reset(){
        mDetectionLines.clear();
    }

    public String processDetection(@NonNull List<DetectedLine> filterResult){
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Log.d(TAG, "mDetections content start>>>>>");

        mProcessDetectonResult.setLength(0);

        for(DetectedLine line : mDetectionLines){
            if(line.update()){
                Log.d(TAG, "Removing " + line.toString());
                mDetectionLines.remove(line);
            }else{
                Log.d(TAG, "Updated " + line.toString());
            }
        }
        Log.d(TAG, "mDetections content end<<<<<<<");

        List<DetectedParcel> inlineParcels;
        int match_index;
        do{
            inlineParcels = getSpareInlineParcels(mFormedParcels);
            if(inlineParcels.size() == 0)
                break;
            DetectedLine detectedLine = new DetectedLine(inlineParcels);
            DetectedLine storedLine   = getInlineDetection(mDetectionLines, detectedLine);

            if(storedLine == null){
                boolean found = false;
                for(DetectedLine line : mDetectionLines){
                    if(line.getLineValue().toUpperCase().equals(detectedLine.getLineValue().toUpperCase())){
                        Log.d(TAG, "Found equal string");
                        detectedLine.mDetectionCounter += line.mDetectionCounter;
                        mDetectionLines.remove(line);
                        mDetectionLines.add(detectedLine);
                        filterResult.add(line);
                        found = true;
                        break;
                    }
                }
                if(!found){
                    mDetectionLines.add(detectedLine);
                    filterResult.add(detectedLine);
                }
                mProcessDetectonResult.append(detectedLine.mLineValue).append('\n');
                continue;
            }

            if((match_index = storedLine.mLineValue.toUpperCase().indexOf(detectedLine.mLineValue.toUpperCase())) > -1){
                Log.d(TAG, "Index matched = " + match_index + " for string" + storedLine.getLineValue());
                storedLine.update(detectedLine, match_index);
                filterResult.add(storedLine);
                mProcessDetectonResult.append(storedLine.mLineValue).append('\n');
            }else{
                boolean found = false;
                Log.d(TAG, "Replacing string=" + storedLine.getLineValue() + " for new string=" + detectedLine.getLineValue());
                mDetectionLines.remove(storedLine);
                for(DetectedLine line : mDetectionLines){
                    if(line.getLineValue().toUpperCase().equals(detectedLine.getLineValue().toUpperCase())){
                        Log.d(TAG, "Found equal string");
                        detectedLine.mDetectionCounter += line.mDetectionCounter;
                        mDetectionLines.remove(line);
                        mDetectionLines.add(detectedLine);
                        filterResult.add(line);
                        found = true;
                        break;
                    }
                }
                if(!found){
                    mDetectionLines.add(detectedLine);
                    filterResult.add(detectedLine);
                }

                mProcessDetectonResult.append(detectedLine.mLineValue).append('\n');
            }
        }while(inlineParcels.size() != 0);

        mFormedParcels.clear();

        Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        return mProcessDetectonResult.toString();
    }


    @NonNull
    private List<DetectedParcel> getSpareInlineParcels(@NonNull SortedSet<DetectedParcel> formedParcels){
        Iterator<DetectedParcel> parcelsIt = formedParcels.iterator();
        ArrayList<DetectedParcel> horizontalIntersections = new ArrayList<>();
        DetectedParcel firstParcel = null;

        while(parcelsIt.hasNext()){
            DetectedParcel parcel = parcelsIt.next();
            if(parcel.mMerged)
                continue;
            if(firstParcel == null){
                firstParcel = parcel;
                parcel.mMerged = true;
                horizontalIntersections.add(firstParcel);
            }else if(firstParcel.isInline(parcel)){
                parcel.mMerged = true;
                horizontalIntersections.add(parcel);
            }
        }

        return horizontalIntersections;
    }

    private DetectedLine getInlineDetection(@NonNull List<DetectedLine> storedLines, DetectedLine receivedLine){
        Rect boundingBox = receivedLine.mBoundingBox;
        int top    = boundingBox.top;
        int bottom = boundingBox.bottom;
        int heightThreshold = boundingBox.height()/2;

        for(DetectedLine line : storedLines){
            if(line.mMerged)
                continue;
            Rect detectedBoundingBox = line.mBoundingBox;
            int topThreshold = Math.abs(detectedBoundingBox.top - top);
            int bottomThreshold = Math.abs(detectedBoundingBox.bottom - bottom);
            if(topThreshold <= heightThreshold && bottomThreshold <= heightThreshold){
                return line;
            }
        }

        return null;
    }

    private void formTextBlockParcels(SortedSet<DetectedParcel> formedParcels, Text textBlock){
        List<? extends Text> list = textBlock.getComponents();
        if(list == null || list.size() == 0){
            char[] value_arr     = textBlock.getValue().toCharArray();
            Rect boundingBox = textBlock.getBoundingBox();
            int char_size = boundingBox.width()/value_arr.length;

            for(int i = 0; i < value_arr.length; i++){
                int left = boundingBox.left + (char_size * i);
                Rect char_box = new Rect(left, boundingBox.top, left+char_size, boundingBox.bottom);
                DetectedParcel detectedText = new DetectedParcel(char_box, value_arr[i]);
//                Log.d(TAG, "Adding text parcel = " + detectedText.toString());
                formedParcels.add(detectedText);
            }

            return;
        }

        for(Text text : list){
            formTextBlockParcels(formedParcels, text);
        }
    }


    private class PositionalComparator implements Comparator<ParcelPosition>{

        @Override
        public int compare(ParcelPosition o1, ParcelPosition o2) {

            if(o1 == null && o2 == null)
                return 0;
            else if(o2 == null)
                return 1;
            else if(o1 == null)
                return -1;

            if(o1.equals(o2))
                return 0;

            int height = o1.compareVertical(o2);
            if(height != 0){
                return height * -1;
            }

            int width = o1.compareHorizontal(o2);
            if(width != 0){
                return width * -1;
            }

            return 0;
        }

    }

    public void setVerticalThreshold(int threshold){
        mVerticalThreshold = threshold;
    }


}
