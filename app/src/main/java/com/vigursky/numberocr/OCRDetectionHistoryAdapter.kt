package com.vigursky.numberocr

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.net.Uri
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import com.vigursky.numberocr.database.DetectionItemModel
import com.vigursky.numberocr.databinding.OcrHistoryItemBinding
import org.greenrobot.eventbus.EventBus

/**
 * Created by vigursky on 29.05.2017.
 */


class OCRDetectionHistoryAdapter(val mContext: Context, val mModels : List<DetectionItemModel>) : RecyclerView.Adapter<OCRDetectionHistoryAdapter.ViewHolder>(){
    val mChecksMap : MutableMap<Int, Boolean> = HashMap()
    init {
        mModels.forEach {
            mChecksMap.put(it.id, false)
        }
    }

    override fun onBindViewHolder(holder: OCRDetectionHistoryAdapter.ViewHolder, position: Int) {
        holder.bindModel(mModels[holder.adapterPosition])
    }

    override fun getItemCount(): Int {
        return mModels.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder{
        val view : View = LayoutInflater.from(parent?.context).inflate(R.layout.ocr_history_item, parent, false)
        val holder = ViewHolder(view)
        return holder
    }

    inner class ViewHolder(mItemView: View) : RecyclerView.ViewHolder(mItemView)
            , View.OnClickListener, PopupMenu.OnMenuItemClickListener {
        val mItemBinding : OcrHistoryItemBinding = DataBindingUtil.bind<OcrHistoryItemBinding>(mItemView)
        var mPopupMenu : PopupMenu = PopupMenu(mItemView.context, mItemView)
        var mItemChecked: Boolean = false
        var modelId = 0

        init {
            mItemBinding.detectionText.setOnClickListener(this@ViewHolder)
            mPopupMenu.menuInflater?.inflate(R.menu.history_action_menu, mPopupMenu.menu)
            mPopupMenu.setOnMenuItemClickListener(this@ViewHolder)
            mItemBinding.chkTrashItem.setOnCheckedChangeListener{_, isChecked -> mChecksMap[modelId] = isChecked}
        }

        fun makePhoneCall(detection : String) : Boolean{
            val rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CALL_PHONE)
            if (rc == PackageManager.PERMISSION_GRANTED) {
                val matchedNumber = Tools.findPhoneNumber(detection) ?: return false
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                callIntent.data = matchedNumber.let{Uri.parse("tel:" + it) }
                mContext.startActivity(callIntent)
            } else {
                EventBus.getDefault().post(OCREvent.Builder()
                        .setEventID(OCREvent.EVENT_REQUEST_PHONE_PERM)
                        .build())
            }
            return true
        }

        fun doOpenURL (detection : String) : Boolean{
            var url: String = Tools.findURL(detection) ?: return false
            if (!url.startsWith("http://") && !url.startsWith("https://"))
                url = "http://" + url
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(browserIntent)
            return true
        }

        fun doCopyBuffer(detection: String): Boolean{
            val text_to_copy = detection.trim()
            val result = Tools.copyToClipboard(mContext, text_to_copy)
            if (result){
                Toast.makeText(mContext, "Copied $text_to_copy to buffer", Toast.LENGTH_SHORT).show()
                return true
            }
            return false
        }

        fun doShareText(detection: String) : Boolean{
            val text_to_send = detection.trim()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text_to_send)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooserIntent = Intent.createChooser(sendIntent, mContext.resources.getString(R.string.share_text))
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(chooserIntent)
            return true
        }

        override fun onClick(v: View?) {
            val detection = mItemBinding.detection.detection
            mPopupMenu.menu.findItem(R.id.item_url).isVisible = false
            mPopupMenu.menu.findItem(R.id.item_call).isVisible = false

            if (Tools.containsPhoneNumber(detection)) {
                mPopupMenu.menu.findItem(R.id.item_call).isVisible = true
                val title = mContext.resources.getString(R.string.call) + " " + Tools.findPhoneNumber(detection)
                mPopupMenu.menu.findItem(R.id.item_call).title = title
            } else if (Tools.containsURL(detection)) {
                mPopupMenu.menu.findItem(R.id.item_url).isVisible = true
                val title = mContext.resources.getString(R.string.open_url) + " " + Tools.findURL(detection)
                mPopupMenu.menu.findItem(R.id.item_url).title = title
            }

            mPopupMenu.show()
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            val detection = mItemBinding.detection.detection
            when(item?.itemId){
                R.id.item_call  -> return makePhoneCall(detection)
                R.id.item_url   -> return doOpenURL(detection)
                R.id.item_copy  -> return doCopyBuffer(detection)
                R.id.item_share -> return doShareText(detection)
                else            -> return false
            }
        }

        fun bindModel(model : DetectionItemModel){
            modelId = model.id
            mItemChecked = mChecksMap[model.id] ?: false
            mItemBinding.chkTrashItem.isChecked = mItemChecked

            mItemBinding.detection = model
            mItemBinding.detectionID.text = String.format("%d.", adapterPosition)
        }

    }
}