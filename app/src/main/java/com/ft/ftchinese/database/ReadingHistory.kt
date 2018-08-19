package com.ft.ftchinese.database


import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.ft.ftchinese.models.ChannelItem

class ReadingHistory(private val db: SQLiteDatabase) {

    fun add(item: ChannelItem) {
        val values = getContentValues(item)
        val result = db.insert(TABLE_NAME, null, values)

        Log.i(TABLE_NAME, "Insert result: $result")
    }

    companion object {
        const val TABLE_NAME = "reading_history"
        const val COL_ID = "id"
        const val COL_TYPE = "type"
        const val COL_SUB_TYPE = "sub_type"
        const val COL_TITLE = "title"
        const val COL_STANDFIRST = "standfirst"

        private fun getContentValues(item: ChannelItem): ContentValues {
            val values = ContentValues()
            values.put(COL_ID, item.id)
            values.put(COL_TYPE, item.type)
            values.put(COL_SUB_TYPE, item.subType)
            values.put(COL_TITLE, item.headline)
            values.put(COL_STANDFIRST, item.shortlead)

            return values
        }
    }
}