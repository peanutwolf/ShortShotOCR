package com.vigursky.numberocr;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

/**
 * Created by vigursky on 24.01.2017.
 */

public class PhoneEditText extends EditText{

    public static final String TAG = PhoneEditText.class.getSimpleName();
    private OnKeyUpListener mKeyUpListener = null;


    public interface OnKeyUpListener{
        boolean onKeyUp(int keyCode, KeyEvent event);
    }

    public PhoneEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PhoneEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PhoneEditText(Context context) {
        super(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PhoneEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setOnKeyUpListener(OnKeyUpListener keyUpListener){
        mKeyUpListener = keyUpListener;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(mKeyUpListener != null){
            return mKeyUpListener.onKeyUp(keyCode, event);
        }else{
            return super.onKeyUp(keyCode, event);
        }
    }
}
