package com.vigursky.numberocr;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.vigursky.numberocr.database.DetectionHistoryDBHelper;
import com.vigursky.numberocr.database.DetectionItemModel;

import java.util.ArrayList;
import com.vigursky.numberocr.database.DetectionItemModelKt;

import static com.vigursky.numberocr.SettingsActivity.TAG_HISTORY_SWITCH_VALUE;

/**
 * Created by vigursky on 09.01.2017.
 */

@RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
public class OCRDialogFragment extends DialogFragment{

    public static final String TAG = OCRDialogFragment.class.getSimpleName();
    public static final String OCR_TEXT_TAG = "com.vigurskiy.numberocr.OCRDialogFragment.OCR_TEXT_TAG";
    public static final String OCR_BITMAP_TAG = "com.vigurskiy.numberocr.OCRDialogFragment.OCR_BITMAP_TAG";

    private Button mOCRCancelButton;
    private ImageView mOCRImageView;
    private RecyclerView mOCRResultRecycler;
    private OCRDetectionListAdapter mDetectionListAdapter;
    private AlertDialog mDialog;
    private View mView;
    private DetectionHistoryDBHelper dbHelper;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String title = getString(R.string.tap_msg);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        ArrayList<CharSequence> numbers = getArguments().getCharSequenceArrayList(OCR_TEXT_TAG);
        Bitmap detectedBitmap = getArguments().getParcelable(OCR_BITMAP_TAG);

        mView = inflater.inflate(R.layout.ocr_result_list, null);
        mOCRCancelButton = (Button) mView.findViewById(R.id.btn_ocr_result_cancel);
        mOCRResultRecycler = (RecyclerView) mView.findViewById(R.id.rv_ocr_result);
        mOCRImageView = (ImageView) mView.findViewById(R.id.imgview_ocr_bitmap);

        if(detectedBitmap != null && !detectedBitmap.isRecycled())
            mOCRImageView.setImageBitmap(detectedBitmap);

        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        boolean histSwitch_def = mSharedPref.getBoolean(TAG_HISTORY_SWITCH_VALUE, true);
        if(numbers != null && histSwitch_def){
            dbHelper = new DetectionHistoryDBHelper(getActivity().getApplicationContext());
            dbHelper.writeDataModels(numbers);
        }
        mDetectionListAdapter = new OCRDetectionListAdapter(getActivity().getApplicationContext(), numbers);

        mOCRResultRecycler.setAdapter(mDetectionListAdapter);
        mOCRResultRecycler.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        mOCRCancelButton.setOnClickListener(v -> OCRDialogFragment.this.dismiss());

        builder.setTitle(title).setView(mView);
        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(true);

        mOCRResultRecycler.post(() -> mDialog.getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM));

        return mDialog;
    }

    interface ICallback{
        void OnDialogDismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if(getActivity() instanceof ICallback){
            ICallback callback = (ICallback) getActivity();
            callback.OnDialogDismiss();
        }
        if(dbHelper != null)
            dbHelper.close();
        super.onDismiss(dialog);
    }

}
