package com.vigursky.numberocr.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.Context
import android.database.Cursor
import com.vigursky.numberocr.R


/**
 * Created by vigursky on 29.05.2017.
 */

class DetectionHistoryDBHelper(val mContext : Context) : SQLiteOpenHelper(mContext, DetectionHistoryDBHelper.DATABASE_NAME, null, DetectionHistoryDBHelper.DATABASE_VERSION) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + DETECTION_TABLE_NAME + " (" +
                DETECTION_COLUMN_ID + " integer primary key autoincrement, " +
                DETECTION_COLUMN_VALUE + " text not null, " +
                DETECTION_COLUMN_TIMESTAMP + " integer not null);")


        if (!triggerExists(DETECTION_LIMIT_TRIGGER_NAME, sqLiteDatabase))
            updateDetectionsLimitTrigger(mContext.resources.getInteger(R.integer.default_history_limit), sqLiteDatabase)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DETECTION_TABLE_NAME)
        onCreate(sqLiteDatabase)
    }

    fun writeToDB(detectionItemModel: DetectionItemModel) : Long{
        val values = ContentValues()
        values.put(DETECTION_COLUMN_VALUE, detectionItemModel.detection)
        values.put(DETECTION_COLUMN_TIMESTAMP, detectionItemModel.timestamp)

        return writableDatabase.insert(DETECTION_TABLE_NAME, null, values)
    }

    private fun readFromDB(): Cursor{
        val projection = arrayOf(
                DETECTION_COLUMN_ID,
                DETECTION_COLUMN_VALUE,
                DETECTION_COLUMN_TIMESTAMP)

        return readableDatabase.query(DETECTION_TABLE_NAME, projection, null, null, null, null, null)
    }

    fun <T> writeDataModels(modelsList : List<T>) : Long{
        var inserted = -1L
        if(modelsList.isEmpty())
            return -1

        when(modelsList[0]) {
            is DetectionItemModel ->
            try {
                writableDatabase.beginTransaction()
                inserted =(modelsList as? List<DetectionItemModel>)
                        ?.count { writeToDB(it) != -1L }
                        ?.toLong()!!
                writableDatabase.setTransactionSuccessful()
            }finally {
                writableDatabase.endTransaction()
                return inserted
            }
            is CharSequence ->{
                val builtModelsList = (modelsList as? List<CharSequence>)?.buildDetectionItemModels() ?: return -1
                return writeDataModels(builtModelsList)
            }
            else -> return -1
        }

    }

    fun <T> readDataModels(readModel:  (Cursor) -> T):List<T>{
        val modelCursor = readFromDB()
        val count = modelCursor.count
        if (count < 0)
            return emptyList()

        modelCursor.moveToPosition(-1)
        val list = generateSequence { if (modelCursor.moveToNext()) modelCursor else null }
                .map { readModel(modelCursor) }
                .toList()
        modelCursor.close()

        return list
    }

    fun deleteFromDB(setIds: Iterable<Int>){
        val ids : String = setIds.joinToString()
        writableDatabase.delete(DETECTION_TABLE_NAME, "$DETECTION_COLUMN_ID IN ($ids)", null)
    }

    fun updateDetectionsLimitTrigger(recordsLimit : Int, sqLiteDatabase: SQLiteDatabase?){
        val databaseHandler = sqLiteDatabase ?: writableDatabase

        val dropTriggerQuery ="DROP TRIGGER IF EXISTS $DETECTION_LIMIT_TRIGGER_NAME;"
        val createTriggerQuery = "CREATE TRIGGER IF NOT EXISTS $DETECTION_LIMIT_TRIGGER_NAME "+
        "AFTER INSERT ON $DETECTION_TABLE_NAME WHEN (select count(*) from $DETECTION_TABLE_NAME) > $recordsLimit "+
        "BEGIN\n"+
        "DELETE FROM $DETECTION_TABLE_NAME WHERE $DETECTION_TABLE_NAME.$DETECTION_COLUMN_ID IN " +
                "(SELECT $DETECTION_TABLE_NAME.$DETECTION_COLUMN_ID FROM $DETECTION_TABLE_NAME " +
                "ORDER BY $DETECTION_TABLE_NAME.$DETECTION_COLUMN_ID " +
                "limit (select count(*) -$recordsLimit from $DETECTION_TABLE_NAME ));\n"+
        "END"

        databaseHandler.execSQL(dropTriggerQuery)
        databaseHandler.execSQL(createTriggerQuery)
    }

    private fun triggerExists(triggerName : String, sqLiteDatabase: SQLiteDatabase?) : Boolean {
        val databaseHandler = sqLiteDatabase ?: writableDatabase
        val selectTriggerQuery = "select * from sqlite_master where type = 'trigger' and name='$triggerName';"

        val cursor = databaseHandler.rawQuery(selectTriggerQuery, null)
        val rawCount = cursor.count
        if (!cursor.isClosed)
            cursor.close()

        return rawCount > 0
    }


    companion object {
        private val DATABASE_VERSION = 1
        val DATABASE_NAME = "ocr_detection_history"
        val DETECTION_TABLE_NAME = "detection_items"
        val DETECTION_COLUMN_ID = "_id"
        val DETECTION_COLUMN_VALUE = "detection"
        val DETECTION_COLUMN_TIMESTAMP = "date"
        const val DETECTION_LIMIT_TRIGGER_NAME = "delete_till_n"
    }
}