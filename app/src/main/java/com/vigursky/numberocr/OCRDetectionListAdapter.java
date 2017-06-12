package com.vigursky.numberocr;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vigursky.numberocr.database.DetectionHistoryDBHelper;
import com.vigursky.numberocr.database.DetectionItemModel;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by vigursky on 22.01.2017.
 */

public class OCRDetectionListAdapter extends RecyclerView.Adapter<OCRDetectionListAdapter.ViewHolder> {

    public static final String TAG = OCRResultAdapter.class.getSimpleName();
    private List<CharSequence> mOCResultValueList;
    private Context mContext;
    private DetectionHistoryDBHelper dbHelper;

    public OCRDetectionListAdapter(Context context, List<CharSequence> resultValues){
        mContext = context;
        mOCResultValueList = resultValues;
        dbHelper = new DetectionHistoryDBHelper(context);
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Log.d(TAG, "onCreateViewHolder");
        Context context = parent.getContext();
        LayoutInflater infalter = LayoutInflater.from(context);

        View OCRItemView = infalter.inflate(R.layout.ocr_detection_item, parent, false);

        return new ViewHolder(OCRItemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        Log.d(TAG, "onBindViewHolder");

        CharSequence value_sequence = mOCResultValueList.get(position);

        holder.mOCRItemValueEdit.setText(value_sequence);

        if(Tools.containsPhoneNumber(value_sequence)){
            holder.mPopupMenu.getMenu().findItem(R.id.item_call).setVisible(true);
            String  title= mContext.getResources().getString(R.string.call) + " " + Tools.findPhoneNumber(value_sequence);
            holder.mPopupMenu.getMenu().findItem(R.id.item_call).setTitle(title);
            holder.mOCRItemValueEdit.setInputType(InputType.TYPE_CLASS_PHONE);
        }else if(Tools.containsURL(value_sequence)){
            holder.mPopupMenu.getMenu().findItem(R.id.item_url).setVisible(true);
            String  title= mContext.getResources().getString(R.string.open_url) + " " + Tools.findURL(value_sequence);
            holder.mPopupMenu.getMenu().findItem(R.id.item_url).setTitle(title);
            holder.mOCRItemValueEdit.setInputType(InputType.TYPE_CLASS_TEXT);
        }
    }

    @Override
    public int getItemCount(){
        return mOCResultValueList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        PhoneEditText mOCRItemValueEdit;
        LinearLayout mOCRItemContainer;
        PopupMenu     mPopupMenu;
        InputMethodManager mInputManager;

        ViewHolder(View itemView) {
            super(itemView);

            Log.d(TAG, "ViewHolder constructor");
            mOCRItemContainer = (LinearLayout) itemView.findViewById(R.id.container_ocr_item_value);
            mOCRItemContainer.setOnTouchListener(new ItemValueEditTouchListener());
            mOCRItemValueEdit = (PhoneEditText) itemView.findViewById(R.id.edtxt_ocr_item_value);
            mOCRItemValueEdit.setOnTouchListener(new ItemValueEditTouchListener());
            mOCRItemValueEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(!hasFocus){
                        setEditState(mOCRItemValueEdit, false);
                        mInputManager.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }else{
                        mInputManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                    }
                }
            });
            mOCRItemValueEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    Editable value_sequence = mOCRItemValueEdit.getText();
                    if(Tools.containsPhoneNumber(value_sequence)){
                        mPopupMenu.getMenu().findItem(R.id.item_call).setVisible(true);
                        String title= mContext.getResources().getString(R.string.call) + " " + Tools.findPhoneNumber(value_sequence);
                        mPopupMenu.getMenu().findItem(R.id.item_call).setTitle(title);
                        mPopupMenu.getMenu().findItem(R.id.item_url).setVisible(false);
                        mOCRItemValueEdit.setInputType(InputType.TYPE_CLASS_PHONE);
                    }else if(Tools.containsURL(value_sequence)){
                        mPopupMenu.getMenu().findItem(R.id.item_url).setVisible(true);
                        String title=mContext.getResources().getString(R.string.open_url) + " " + Tools.findURL(value_sequence);
                        mPopupMenu.getMenu().findItem(R.id.item_url).setTitle(title);
                        mPopupMenu.getMenu().findItem(R.id.item_call).setVisible(false);
                        mOCRItemValueEdit.setInputType(InputType.TYPE_CLASS_TEXT);
                    }else{
                        mPopupMenu.getMenu().findItem(R.id.item_url).setVisible(false);
                        mPopupMenu.getMenu().findItem(R.id.item_call).setVisible(false);
                        mOCRItemValueEdit.setInputType(InputType.TYPE_CLASS_TEXT);
                    }
                    setEditState(mOCRItemValueEdit, false);
                    mInputManager.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    return true;
                }
            });
            mInputManager = (InputMethodManager) mOCRItemValueEdit.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            mPopupMenu = new PopupMenu(itemView.getContext(), itemView);
            mPopupMenu.getMenuInflater().inflate(R.menu.phone_action_menu, mPopupMenu.getMenu());
            mPopupMenu.setOnMenuItemClickListener(new PopupMenuClickListener());
        }

        private void saveToHistory(){
            String textToSave = mOCRItemValueEdit.getText().toString().trim();
            long res = dbHelper.writeToDB(new DetectionItemModel(-1, textToSave, 0));
            if(res != -1){
                Toast toast = Toast.makeText(mContext, "Saved " + textToSave + " to history", Toast.LENGTH_SHORT);
                toast.show();
            }
        }

        private class ItemValueEditTouchListener implements View.OnTouchListener{
            @Override
            public boolean onTouch(View v, MotionEvent event){
                switch (event.getAction()){
                    case MotionEvent.ACTION_UP:
                        if(!mOCRItemValueEdit.hasFocus()){
                            mPopupMenu.show();
                            return true;
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        }

        private class PopupMenuClickListener implements PopupMenu.OnMenuItemClickListener{
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.item_edit:
                        setEditState(mOCRItemValueEdit, true);
                        mOCRItemValueEdit.requestFocus();
                        return true;
                    case R.id.item_call:
                        int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CALL_PHONE);
                        if (rc == PackageManager.PERMISSION_GRANTED) {
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                            String matchedNumber = Tools.findPhoneNumber(mOCRItemValueEdit.getText().toString().trim());
                            callIntent.setData(Uri.parse("tel:" + matchedNumber));
                            mContext.startActivity(callIntent);
                        } else {
                            EventBus.getDefault().post(new OCREvent.Builder()
                                    .setEventID(OCREvent.EVENT_REQUEST_PHONE_PERM)
                                    .build());
                        }
                        return true;
                    case R.id.item_copy:
                        String text_to_copy = mOCRItemValueEdit.getText().toString().trim();
                        boolean result = Tools.copyToClipboard(mOCRItemValueEdit.getContext(), text_to_copy);
                        if (result){
                            Toast toast = Toast.makeText(mContext, "Copied " + text_to_copy + " to buffer", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        return true;
                    case R.id.item_url:
                        String url = Tools.findURL(mOCRItemValueEdit.getText().toString().trim());
                        if(url == null)
                            return false;
                        if (!url.startsWith("http://") && !url.startsWith("https://"))
                            url = "http://" + url;
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(browserIntent);
                        return true;
                    case R.id.item_share:
                        String text_to_send = mOCRItemValueEdit.getText().toString().trim();
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_TEXT, text_to_send)
                        .setType("text/plain")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Intent chooserIntent = Intent.createChooser(sendIntent, mContext.getResources().getString(R.string.share_text));
                        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(chooserIntent);
                        return true;
                    case R.id.item_save:
                        saveToHistory();
                        return true;
                    default:
                        return false;
                }
            }
        }

        private void setEditState(EditText editView, boolean editState){
            if(editState){
                editView.setFocusableInTouchMode(true);
                editView.setFocusable(true);
                editView.setClickable(true);
                editView.setCursorVisible(true);
            }else{
                editView.setFocusableInTouchMode(false);
                editView.setFocusable(false);
                editView.setClickable(false);
                editView.setCursorVisible(false);
            }
        }
    }
}
