package com.vigursky.numberocr;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.util.List;

/**
 * Created by vigursky on 22.01.2017.
 */

@Deprecated
public class OCRResultAdapter extends RecyclerView.Adapter<OCRResultAdapter.ViewHolder> {

    public static final String TAG = OCRResultAdapter.class.getSimpleName();
    private List<CharSequence> mOCResultValueList;
    private Context mContext;
    private ItemClickListener mClickListener;
    private int mEditablePosition;
    private String mEditedValue;
    private ItemValueWatcher mItemValueWatcher;

    public String getItemValue(int position) {
        return mOCResultValueList.get(position).toString();
    }

    public interface ItemClickListener{
        void onItemClick(View view, int position);
    }

    public OCRResultAdapter(Context context, List<CharSequence> resultValues){
        mContext = context;
        mOCResultValueList = resultValues;
        mItemValueWatcher = new ItemValueWatcher();
        mEditablePosition = -1;
    }

    public void setClickListener(ItemClickListener listener){
        mClickListener = listener;
    }

    public void setEditablePosition(int position){
        mEditablePosition = position;
    }

    public void storeEditChange(){
        if(mEditablePosition != -1){
            mOCResultValueList.set(mEditablePosition, mEditedValue);
        }
    }

    public int getEditablePosition(){
        return mEditablePosition;
    }

    public void resetEditing(){
        if(mEditablePosition != -1){
            int editable_pos = mEditablePosition;
            OCRResultAdapter.this.setEditablePosition(-1);
            OCRResultAdapter.this.notifyItemChanged(editable_pos);
        }
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        Log.d(TAG, "onCreateViewHolder");
        Context context = parent.getContext();
        LayoutInflater infalter = LayoutInflater.from(context);

        View OCRItemView = infalter.inflate(R.layout.ocr_result_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(OCRItemView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        Log.d(TAG, "onBindViewHolder");

        if(position == mEditablePosition){
            if(!holder.mEditableIsShown){
                holder.mOCRItemSwitcher.showNext();
                holder.mEditableIsShown = true;
            }
            holder.mOCRItemValueEdit.setText(mOCResultValueList.get(position));
            holder.mOCRItemValueEdit.requestFocus();
        }else{
            if(holder.mEditableIsShown){
                holder.mOCRItemSwitcher.showPrevious();
                holder.mEditableIsShown = false;
            }
            holder.mOCRItemValue.setText(mOCResultValueList.get(position));
        }
    }

    @Override
    public int getItemCount(){
        return mOCResultValueList.size();
    }

    class ItemValueWatcher implements TextWatcher{
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mEditedValue = s.toString();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        TextView mOCRItemValue;
        ViewSwitcher mOCRItemSwitcher;
        PhoneEditText mOCRItemValueEdit;

        boolean mEditableIsShown = false;

        ViewHolder(View itemView) {
            super(itemView);

            Log.d(TAG, "ViewHolder constructor");

            mOCRItemValue = (TextView) itemView.findViewById(R.id.txt_ocr_item_value);
            mOCRItemSwitcher = (ViewSwitcher) itemView.findViewById(R.id.vswch_ocr_item);
            mOCRItemValueEdit = (PhoneEditText) itemView.findViewById(R.id.edtxt_ocr_item_value);
            mOCRItemValueEdit.addTextChangedListener(mItemValueWatcher);
            mOCRItemValueEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    InputMethodManager imm = (InputMethodManager) mOCRItemValueEdit.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(hasFocus){
                        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
                    }else{
                        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }
            });
            mOCRItemValueEdit.setOnKeyUpListener(new PhoneEditText.OnKeyUpListener() {
                @Override
                public boolean onKeyUp(int keyCode, KeyEvent event) {
                    if(keyCode == KeyEvent.KEYCODE_ENTER){
                        int editable_pos = OCRResultAdapter.this.getEditablePosition();
                        OCRResultAdapter.this.storeEditChange();
                        OCRResultAdapter.this.setEditablePosition(-1);
                        OCRResultAdapter.this.notifyItemChanged(editable_pos);
                    }
                    return true;
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mClickListener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            mClickListener.onItemClick(v, position);
                        }
                    }

                }
            });
        }
    }
}
