package com.vigursky.numberocr;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;
import java.util.ListIterator;

/**
 * Created by vigursky on 26.01.2017.
 */

public class OCRGraphicView extends View {
    private Paint mTextPaint = new Paint();
    private FocusBoxView mFocusBox;
    private List<OCRPositionalFilter.DetectedLine> mLinesToDisplay;
    private FocusBoxView.FocusBoxTouchListener mFocusBoxTouchListener = (rect, event) -> OCRGraphicView.this.invalidate();

    public OCRGraphicView(Context context) {
        super(context);
    }

    public OCRGraphicView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OCRGraphicView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OCRGraphicView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    public void drawGraphics(@Nullable  List<OCRPositionalFilter.DetectedLine> lines){
        this.mLinesToDisplay = lines;
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Resources resources = getResources();
        Rect focusBox;
        ListIterator<OCRPositionalFilter.DetectedLine> detectedLineIterator;

        if(mFocusBox == null)
            return;

        mTextPaint.setTextSize(resources.getDimension(R.dimen.detectedFont_size));
        mTextPaint.setColor(resources.getColor(R.color.colorLtGray));
        focusBox = mFocusBox.getBox();
        float lineInterval = resources.getDimension(R.dimen.detectedFont_interval);
        int textPosLeft = focusBox.left;
        int textPosTop  = focusBox.top - (int)lineInterval;
        if(mLinesToDisplay == null || mLinesToDisplay.isEmpty()){
            mTextPaint.setTextSize(resources.getDimension(R.dimen.SettingsText_size));
            String idle_text = getResources().getString(R.string.focus_box_text);
            canvas.drawText(idle_text, textPosLeft, textPosTop, mTextPaint);
        }else{
            detectedLineIterator = mLinesToDisplay.listIterator(mLinesToDisplay.size());
            while(detectedLineIterator.hasPrevious()){
                OCRPositionalFilter.DetectedLine detectedLine = detectedLineIterator.previous();
                canvas.drawText(detectedLine.getLineValue(), textPosLeft, textPosTop, mTextPaint);
                textPosTop -= (mTextPaint.getTextSize() + lineInterval);
            }
        }
    }

    public void setFocusBox(final FocusBoxView focusBox) {
        focusBox.addOnFocusBoxMoveListener(mFocusBoxTouchListener);
        this.mFocusBox = focusBox;
    }

}
