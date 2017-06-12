package com.vigursky.numberocr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.telephony.PhoneNumberUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.Display;
import android.view.Surface;
import com.google.i18n.phonenumbers.*;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.LongUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by vigursky on 23.12.2016.
 */
public class Tools {
    public static final String TAG = Tools.class.getSimpleName()+"Class";
    private static Matrix mat = new Matrix();
    private static PhoneNumberUtil mPhoneUtil = PhoneNumberUtil.getInstance();

    public static Bitmap getFocusedBitmap(Context context, Camera camera, byte[] data, Rect box) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        Point ScrRes = new Point(metrics.widthPixels, metrics.heightPixels);

        Bitmap bmp1 = BitmapFactory.decodeByteArray(data, 0, data.length);
        if(bmp1 == null){
            Log.d(TAG, "Cannot decode bitmap");
            return null;
        }

        Point CamRes = new Point(camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height);

        double SW = ScrRes.x;
        double SH = ScrRes.y;

        double CH = CamRes.x;
        double CW = CamRes.y;

        double kw = (CW/SW);
        double kh = (CH/SH);

//        kh += 0.04;

        Rect scaledRect = new Rect((int)Math.round(box.left * kw), (int)Math.round(box.top * kh),
                (int)Math.round(box.right * kw), (int)Math.round(box.bottom * kh));

        int RL = scaledRect.left;
        int RT = scaledRect.top;
        int RW = scaledRect.width();
        int RH = scaledRect.height();

        int RL_trans = RT;
        int RT_trans = bmp1.getHeight() - RW - RL;
        int RW_trans = RH;
        int RH_trans = RW;

        Bitmap bmp = null;
        Matrix mat = new Matrix();
        mat.postRotate(90);
        try{
            bmp = Bitmap.createBitmap(bmp1, RL_trans, RT_trans, RW_trans, RH_trans, mat, true);
        }catch (Exception e){
            e.printStackTrace();
        }
        bmp1.recycle();
        return bmp;

    }

    public static Bitmap toBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data , 0, data.length);
    }

    public static Bitmap rotate(Bitmap in, int angle) {
        mat.setRotate(angle);
        return Bitmap.createBitmap(in, 0, 0, in.getWidth(), in.getHeight(), mat, true);
    }

    public static void transpone(Rect rect, @FocusBoxView.SurfaceRotation int rotation, int previewWidth, int previewHeight){
        int RL_trans, RL = rect.left;
        int RT_trans, RT = rect.top;
        int RR_trans, RR = rect.right;
        int RB_trans, RB = rect.bottom;

        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
            RL_trans = RT;
            RT_trans = previewHeight - RR;
            RR_trans = RB;
            RB_trans = previewHeight  - RL;
        }else{
            RL_trans = RL;
            RT_trans = RT;
            RR_trans = RR;
            RB_trans = RB;
        }

        rect.set(RL_trans, RT_trans, RR_trans, RB_trans);
    }

    public static void resize(Rect rect, int resize_value){
        rect.set(rect.left+resize_value, rect.top+resize_value,
                rect.right+resize_value, rect.bottom+resize_value);
    }

    public static void bound(Rect rect, int bound, BiPredicate<Integer, Integer> funcBound){
        if(funcBound.test(rect.left, bound))
            rect.left = bound;
        if(funcBound.test(rect.top, bound))
            rect.top = bound;
        if(funcBound.test(rect.right, bound))
            rect.right = bound;
        if(funcBound.test(rect.bottom, bound))
            rect.bottom = bound;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static boolean copyToClipboard(Context context, String text) {

        if(context == null || text == null)
            return false;

        try {
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                        .getSystemService(context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                        .getSystemService(context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData
                        .newPlainText(
                                context.getResources().getString(
                                        R.string.clipboard_label), text);
                clipboard.setPrimaryClip(clip);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPhoneValidNumber(CharSequence phoneNumber, String countryCode){
        Iterable<PhoneNumberMatch> matchedNumbers
                =mPhoneUtil.findNumbers(phoneNumber, countryCode, PhoneNumberUtil.Leniency.POSSIBLE, Long.MAX_VALUE);
        if(matchedNumbers.iterator().hasNext())
            return true;

        return Patterns.PHONE.matcher(phoneNumber).matches();
    }

    public static boolean isPhoneValidNumber(CharSequence phoneNumber){
        String defCountry = Locale.getDefault().getCountry();

        return isPhoneValidNumber(phoneNumber, defCountry);
    }

    public static boolean containsPhoneNumber(CharSequence phoneNumber, String countryCode){
        Iterable<PhoneNumberMatch> matchedNumbers
                =mPhoneUtil.findNumbers(phoneNumber, countryCode, PhoneNumberUtil.Leniency.POSSIBLE, Long.MAX_VALUE);
        Iterator<PhoneNumberMatch> it = matchedNumbers.iterator();
        if(it.hasNext())
            return true;

        return Patterns.PHONE.matcher(phoneNumber).find();
    }

    public static boolean containsPhoneNumber(CharSequence phoneNumber){
        String defCountry = Locale.getDefault().getCountry();

        return containsPhoneNumber(phoneNumber, defCountry);
    }

    public static boolean isValidURL(CharSequence url){
        return Patterns.WEB_URL.matcher(url).matches();
    }

    public static boolean containsURL(CharSequence url){
        return Patterns.WEB_URL.matcher(url).find();
    }

    @Nullable
    public static String findPhoneNumber(CharSequence stringWithPhone) {
        String defCountry = Locale.getDefault().getCountry();

        return findPhoneNumber(stringWithPhone, defCountry);
    }

    @Nullable
    public static String findPhoneNumber(CharSequence stringWithPhone, String countryCode) {
        Iterable<PhoneNumberMatch> matchedNumbers
                =mPhoneUtil.findNumbers(stringWithPhone, countryCode, PhoneNumberUtil.Leniency.POSSIBLE, Long.MAX_VALUE);
        Iterator<PhoneNumberMatch> it = matchedNumbers.iterator();
        if(it.hasNext())
            return it.next().rawString();

        Matcher m = Patterns.PHONE.matcher(stringWithPhone);
        if(m.find()){
            return m.group(0);
        }

        return null;
    }

    @Nullable
    public static String findURL(CharSequence stringWithURL) {
        Matcher m = Patterns.WEB_URL.matcher(stringWithURL);
        if(m.find()){
            return m.group(0);
        }
        return null;
    }
}
