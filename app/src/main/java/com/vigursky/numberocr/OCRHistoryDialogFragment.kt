package com.vigursky.numberocr

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View.*
import com.vigursky.numberocr.database.DetectionHistoryDBHelper
import com.vigursky.numberocr.database.buildDetectionModel
import com.vigursky.numberocr.databinding.OcrHistoryListBinding
import java.util.function.BiConsumer

/**
 * Created by vigursky on 02.06.2017.
 */


class OCRHistoryDialogFragment : DialogFragment(){
    var listBinding : OcrHistoryListBinding? = null
    var mDetectionDbHelper : DetectionHistoryDBHelper? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        mDetectionDbHelper = DetectionHistoryDBHelper(activity.applicationContext)

        listBinding = DataBindingUtil.inflate(
                LayoutInflater.from(activity.applicationContext),
                R.layout.ocr_history_list, null, false)

        val mDetectionsList = mDetectionDbHelper?.readDataModels(::buildDetectionModel)?.toMutableList()
        if(mDetectionsList == null || mDetectionsList.count() <= 0){
            listBinding?.rvOcrHistory?.visibility = GONE
            listBinding?.txtOcrHistoryEmpty?.visibility = VISIBLE
        }else{
            listBinding?.rvOcrHistory?.adapter = OCRDetectionHistoryAdapter(activity.applicationContext, mDetectionsList)
            listBinding?.rvOcrHistory?.layoutManager = LinearLayoutManager(activity)
        }
        listBinding?.btnOcrHistoryCancel?.setOnClickListener { this.dismiss() }
        listBinding?.bntTrash?.setOnClickListener{
            val adapter = listBinding?.rvOcrHistory?.adapter as OCRDetectionHistoryAdapter? ?: return@setOnClickListener
            val idsToDelete: Iterable<Int> = adapter.mChecksMap
                    .filterValues { isChecked -> isChecked }
                    .keys
                    .forEachAction {
                        modelIdKey -> val checkedModel = mDetectionsList?.find { it.id == modelIdKey }
                        mDetectionsList?.remove(checkedModel)
                    }.forEachAction {
                        modelIdKey -> adapter.mChecksMap.remove(modelIdKey)
                    }
            if(idsToDelete.count() > 0){
                listBinding?.chkTrashAll?.isChecked = false
                mDetectionDbHelper?.deleteFromDB(idsToDelete)
                adapter.notifyDataSetChanged()
            }
        }
        listBinding?.chkTrashAll?.setOnCheckedChangeListener { _, isChecked ->
            val adapter = listBinding?.rvOcrHistory?.adapter as OCRDetectionHistoryAdapter? ?: return@setOnCheckedChangeListener
            for(e in adapter.mChecksMap)
                e.setValue(isChecked)
            adapter.notifyDataSetChanged()
        }

        val dialog = builder.setView(listBinding?.root).create()
        dialog.setCanceledOnTouchOutside(true)

        return dialog
    }

    private fun <T> Iterable<T>.forEachAction(action: (T) -> Unit): Iterable<T> {
        for (element in this) action(element)
        return this
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        if (activity is OCRDialogFragment.ICallback) {
            val callback = activity as OCRDialogFragment.ICallback
            callback.OnDialogDismiss()
        }
    }

}