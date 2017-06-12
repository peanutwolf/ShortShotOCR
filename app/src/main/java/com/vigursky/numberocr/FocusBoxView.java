package com.vigursky.numberocr;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.StringDef;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FocusBoxView extends View {
    public static final String TAG = FocusBoxView.class.getSimpleName();
    public static final int    BOX_CORNER_RADIUS = 16;
    public static final int    BOX_CORNER_TOUCH_AREA = 32;
    private static final int   MIN_FOCUS_BOX_WIDTH = 80;
    private static final int   MIN_FOCUS_BOX_HEIGHT = 50;

    private final Paint paint;
    private final int maskColor;
    private final int cornerColor;

    private Paint mBackgroundPaint;
    private Rect box;
    private Rect mScaledBox;
    private static Point ScrRes;

    private int mPreviewWidth;
    private int mPreviewHeight;
    private Set<FocusBoxTouchListener> mFocusBoxMoveListeners = new HashSet<>();

    public void addOnFocusBoxMoveListener(FocusBoxTouchListener onFocusBoxMoveListener) {
        mFocusBoxMoveListeners.add(onFocusBoxMoveListener);
    }

    @IntDef({
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
    })
    @Retention(RetentionPolicy.SOURCE)
    public  @interface SurfaceRotation {}

    private enum BoxArea{
        OUTSIDE(-1),
        LEFT_TOP_CORNER(0),
        LEFT_BOTTOM_CONRNER(1),
        RIGHT_TOP_CORNER(2),
        RIGHT_BOTTOM_CONRER(3),
        INSIDE(4);

        private final int mId;

        BoxArea(int id){
            mId = id;
        }

    }

    public interface FocusBoxTouchListener{
        void FocusMoved(Rect rect, MotionEvent event);
    }

    public FocusBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();

        maskColor = Color.parseColor("#D20E0F02");
        cornerColor = resources.getColor(R.color.colorPrimaryDark);

        this.setOnTouchListener(getTouchListener());
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        ScrRes = new Point(getWidth(), getHeight());
        if(mPreviewWidth == 0 || mPreviewHeight == 0){
            mPreviewWidth = ScrRes.x;
            mPreviewHeight = ScrRes.y;
        }
    }

    private void init() {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void setPreviewSize(int previewWidth, int previewHeight){
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
    }

    private  Rect getBoxRect() {

        if (box == null) {

            int screen_x = Math.min(ScrRes.x, ScrRes.y);
            int screen_y = Math.max(ScrRes.x, ScrRes.y);

            int width = screen_x * 6 / 7;
            int height = screen_y / 9;

            width = width == 0
                    ? MIN_FOCUS_BOX_WIDTH
                    : width < MIN_FOCUS_BOX_WIDTH ? MIN_FOCUS_BOX_WIDTH : width;

            height = height == 0
                    ? MIN_FOCUS_BOX_HEIGHT
                    : height < MIN_FOCUS_BOX_HEIGHT ? MIN_FOCUS_BOX_HEIGHT : height;

            int left = (ScrRes.x - width) / 2;
            int top = (ScrRes.y - height) / 2;

            box = new Rect(left, top, left + width, top + height);
        }

        return box;
    }

    public Point getScreenResolution(){
        return ScrRes;
    }

    public Rect getPreviewScaledBox(@SurfaceRotation int rotation){
        Rect   focus_box = getBoxRect();
        double RL = focus_box.left;
        double RT = focus_box.top;
        double RR = focus_box.right;
        double RB = focus_box.bottom;
        int RL_trans=(int)RL, RT_trans=(int)RT, RR_trans=(int)RR, RB_trans=(int)RB;

        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
            double kh = (double)mPreviewWidth / (double)ScrRes.y;
            double kw = (double)mPreviewHeight / (double)ScrRes.x;

            RL_trans = (int)Math.round(RT * kh);
            RT_trans = (int)Math.round(mPreviewHeight - (RR * kw));
            RR_trans = (int)Math.round(RB * kh);
            RB_trans = (int)Math.round(mPreviewHeight  - (RL * kw));
        }else if(rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_0){
            double kw = (double)mPreviewWidth/(double)ScrRes.x;
            double kh = (double)mPreviewHeight/(double)ScrRes.y;

            RL_trans = (int)Math.round(RL * kw);
            RT_trans = (int)Math.round(RT * kh);
            RR_trans = (int)Math.round(RR * kw);
            RB_trans = (int)Math.round(RB * kh);
        }

        if(mScaledBox == null){
            mScaledBox = new Rect(RL_trans, RT_trans, RR_trans, RB_trans);
        }else{
            mScaledBox.set(RL_trans, RT_trans, RR_trans, RB_trans);
        }

        return mScaledBox;
    }

    public Rect getBox() {
        return box;
    }

    private void moveBoxRect(int dW, int dH) {
        int newLeft = (box.left + dW) < 0 ? 0 : box.left + dW;
        int newRight = (box.right + dW) >  ScrRes.x ? ScrRes.x : box.right + dW;
        int newTop = (box.top + dH) < 0 ? 0 : box.top + dH;
        int newBottom = (box.bottom + dH) >  ScrRes.y ? ScrRes.y : box.bottom + dH;

        if(dW <= 0)
            newRight = newLeft + box.width();
        else if(dW > 0)
            newLeft = newRight - box.width();
        if(dH <= 0)
            newBottom = newTop + box.height();
        else if(dH > 0)
            newTop = newBottom - box.height();

        box.set(newLeft, newTop, newRight, newBottom);
    }


    private void resizeBoxRect(int dW, int dH, BoxArea corner) {
        int newLeft = box.left;
        int newRight = box.right;
        int newTop = box.top ;
        int newBottom = box.bottom;

        switch (corner){
            case LEFT_TOP_CORNER:
                if(dW <= 0)
                    newLeft = (box.left + dW) <= 0 ? 0 : box.left + dW;
                else
                    newLeft = (box.width() - dW) <= MIN_FOCUS_BOX_WIDTH ? box.right - MIN_FOCUS_BOX_WIDTH : box.left + dW;
                if(dH <= 0)
                    newTop = (box.top + dH) <= 0 ? 0 : box.top + dH;
                else
                    newTop = (box.height() - dH) <= MIN_FOCUS_BOX_HEIGHT ? box.bottom - MIN_FOCUS_BOX_HEIGHT : box.top + dH;
                break;
            case LEFT_BOTTOM_CONRNER:
                if(dW <= 0)
                    newLeft = (box.left + dW) <= 0 ? 0 : box.left + dW;
                else
                    newLeft = (box.width() - dW) <= MIN_FOCUS_BOX_WIDTH ? box.right - MIN_FOCUS_BOX_WIDTH : box.left + dW;
                if(dH <= 0)
                    newBottom = (box.height() + dH) <= MIN_FOCUS_BOX_HEIGHT ? box.top + MIN_FOCUS_BOX_HEIGHT : box.bottom + dH;
                else
                    newBottom = (box.bottom + dH) >= ScrRes.y ? ScrRes.y : box.bottom + dH;
                break;
            case RIGHT_TOP_CORNER:
                if(dW <= 0)
                    newRight = (box.width() + dW) <= MIN_FOCUS_BOX_WIDTH ? box.left + MIN_FOCUS_BOX_WIDTH : box.right + dW;
                else
                    newRight = (box.right + dW) >= ScrRes.x ? ScrRes.x : box.right + dW;
                if(dH <= 0)
                    newTop = (box.top + dH) <= 0 ? 0 : box.top + dH;
                else
                    newTop = (box.height() - dH) <= MIN_FOCUS_BOX_HEIGHT ? box.bottom - MIN_FOCUS_BOX_HEIGHT : box.top + dH;
                break;
            case RIGHT_BOTTOM_CONRER:
                if(dW <= 0)
                    newRight = (box.width() + dW) <= MIN_FOCUS_BOX_WIDTH ? box.left + MIN_FOCUS_BOX_WIDTH : box.right + dW;
                else
                    newRight = (box.right + dW) >= ScrRes.x ? ScrRes.x : box.right + dW;
                if(dH <= 0)
                    newBottom = (box.height() + dH) <= MIN_FOCUS_BOX_HEIGHT ? box.top + MIN_FOCUS_BOX_HEIGHT : box.bottom + dH;
                else
                    newBottom = (box.bottom + dH) >= ScrRes.y ? ScrRes.y : box.bottom + dH;
        }

        box.set(newLeft, newTop, newRight, newBottom);

    }

    private OnTouchListener touchListener;

    private OnTouchListener getTouchListener() {

        if (touchListener == null)
            touchListener = new OnTouchListener() {

                int lastX = -1;
                int lastY = -1;
                Rect cornerRect = new Rect();

                private BoxArea updateType(Rect box){

                    if(box == null)
                        return BoxArea.OUTSIDE;

                    if(box.left > box.right && box.top > box.bottom)
                        return BoxArea.OUTSIDE;

                    cornerRect.set(box.left - BOX_CORNER_TOUCH_AREA, box.top - BOX_CORNER_TOUCH_AREA, box.left + BOX_CORNER_TOUCH_AREA, box.top + BOX_CORNER_TOUCH_AREA);
                    if(cornerRect.contains(lastX, lastY))
                        return BoxArea.LEFT_TOP_CORNER;
                    cornerRect.set(box.left - BOX_CORNER_TOUCH_AREA, box.bottom - BOX_CORNER_TOUCH_AREA, box.left + BOX_CORNER_TOUCH_AREA, box.bottom + BOX_CORNER_TOUCH_AREA);
                    if(cornerRect.contains(lastX, lastY))
                        return BoxArea.LEFT_BOTTOM_CONRNER;
                    cornerRect.set(box.right - BOX_CORNER_TOUCH_AREA, box.top - BOX_CORNER_TOUCH_AREA, box.right + BOX_CORNER_TOUCH_AREA, box.top + BOX_CORNER_TOUCH_AREA);
                    if(cornerRect.contains(lastX, lastY))
                        return BoxArea.RIGHT_TOP_CORNER;
                    cornerRect.set(box.right - BOX_CORNER_TOUCH_AREA, box.bottom - BOX_CORNER_TOUCH_AREA, box.right + BOX_CORNER_TOUCH_AREA, box.bottom + BOX_CORNER_TOUCH_AREA);
                    if(cornerRect.contains(lastX, lastY))
                        return BoxArea.RIGHT_BOTTOM_CONRER;
                    if(box.contains(lastX, lastY))
                        return BoxArea.INSIDE;

                    return BoxArea.OUTSIDE;
                }

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int currentX = (int) event.getX();
                    int currentY = (int) event.getY();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = currentX;
                            lastY = currentY;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            Rect box = getBox();
                            BoxArea updateType = updateType(box);
                            if(updateType == BoxArea.OUTSIDE)
                                return true;
                            else if(updateType == BoxArea.INSIDE)
                                moveBoxRect(currentX - lastX, currentY - lastY);
                            else
                                resizeBoxRect(currentX - lastX, currentY - lastY, updateType);
                            FocusBoxView.this.invalidate();
                            lastX = currentX;
                            lastY = currentY;
                            return true;
                        case MotionEvent.ACTION_UP:
                            for(FocusBoxTouchListener l : mFocusBoxMoveListeners){
                                l.FocusMoved(getBox(), event);
                            }
                            lastX = -1;
                            lastY = -1;
                            return true;
                    }
                    return false;
            }
    };

    return touchListener;
}

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = getBoxRect();

        canvas.drawColor(maskColor);
        canvas.drawRect(frame, mBackgroundPaint);

        paint.setColor(cornerColor);

        canvas.drawCircle(frame.left, frame.top, BOX_CORNER_RADIUS, paint);
        canvas.drawCircle(frame.right, frame.top, BOX_CORNER_RADIUS, paint);
        canvas.drawCircle(frame.left, frame.bottom, BOX_CORNER_RADIUS, paint);
        canvas.drawCircle(frame.right, frame.bottom, BOX_CORNER_RADIUS, paint);

    }

}