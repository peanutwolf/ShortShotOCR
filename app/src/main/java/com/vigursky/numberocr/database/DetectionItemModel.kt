package com.vigursky.numberocr.database

import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException

/**
 * Created by vigursky on 29.05.2017.
 */

data class DetectionItemModel(val id : Int, val detection : String, val timestamp: Int)

fun List<CharSequence>.buildDetectionItemModels() : List<DetectionItemModel>{
    return map { DetectionItemModel(-1, it.toString(), 0) }
}

fun buildDetectionModel(cursor: Cursor) : DetectionItemModel{
    return try {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionHistoryDBHelper.DETECTION_COLUMN_ID))
        val value = cursor.getString(cursor.getColumnIndexOrThrow(DetectionHistoryDBHelper.DETECTION_COLUMN_VALUE))
        val time = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionHistoryDBHelper.DETECTION_COLUMN_TIMESTAMP))

        DetectionItemModel(id, value, time)
    }catch (e : Exception){
        when(e){
            is IllegalArgumentException,
            is CursorIndexOutOfBoundsException -> {
                return DetectionItemModel(0, "NONE", 0)
            }
            else -> return DetectionItemModel(0, "NONE", 0)
        }
    }
}

class CursorModelAdapter(val cursor: Cursor){
    fun <T> getModel(position : Int, read:  (Cursor) -> T) : T{
        if(cursor.count > position)
            cursor.moveToPosition(position)
        return read(cursor)
    }
}