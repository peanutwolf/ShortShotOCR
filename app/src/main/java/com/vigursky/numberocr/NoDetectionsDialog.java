package com.vigursky.numberocr;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

/**
 * Created by vigursky on 03.03.2017.
 */

public class NoDetectionsDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getString(R.string.detections);
        String message = getString(R.string.empty_detections);
        String cancel = getString(R.string.cancel);

        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setMessage(message)
                .setNeutralButton(cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NoDetectionsDialog.this.dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if(getActivity() instanceof OCRDialogFragment.ICallback){
            OCRDialogFragment.ICallback callback = (OCRDialogFragment.ICallback) getActivity();
            callback.OnDialogDismiss();
        }
        super.onDismiss(dialog);
    }

}
