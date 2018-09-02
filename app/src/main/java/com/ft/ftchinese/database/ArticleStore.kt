package com.ft.ftchinese.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import android.util.Log
import com.ft.ftchinese.models.ChannelItem

class ArticleStore private constructor(context: Context){

    private val mDatabase: SQLiteDatabase = ArticleDbHelper.getInstance(context)
            .writableDatabase

    fun addHistory(item: ChannelItem?) {
        if (item == null) return

        val values = contentValues(item)
        val result = mDatabase.insert(HistoryTable.NAME, null, values)

        Log.i(TAG, "Insert result: $result")
    }

    fun queryHistory(whereClause: String? = null, whereArgs: Array<String>? = null): ArticleCursorWrapper {
        val cursor = mDatabase.query(
                HistoryTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                "${BaseColumns._ID} DESC"
        )

        return ArticleCursorWrapper(cursor)
    }

    fun addStarred(item: ChannelItem?) {
        if (item == null) return

        val values = contentValues(item)
        val result = mDatabase.insert(StarredTable.NAME, null, values)

        Log.i(TAG, "Starred an article: $result")
    }

    fun deleteStarred(item: ChannelItem?) {
        if (item == null) return

        val id = item.id
        val result = mDatabase.delete(StarredTable.NAME, "${StarredTable.Cols.ID} = ?", arrayOf(id))
        Log.i(TAG, "Delete starred article $item, result: $result")
    }

    fun isStarring(item: ChannelItem?): Boolean {
        if (item == null) return false
        val id = item.id
        val query = """
            SELECT EXISTS(
                SELECT *
                FROM ${StarredTable.NAME}
                WHERE ${StarredTable.Cols.ID} = ?
                LIMIT 1
            )
        """.trimIndent()

        val cursor = mDatabase.rawQuery(query, arrayOf(id))

        cursor.moveToFirst()
        val exists = cursor.getInt(0)

        cursor.close()
        return exists == 1
    }

    fun queryStarred(whereClause: String? = null, whereArgs: Array<String>? = null): ArticleCursorWrapper {
        val cursor = mDatabase.query(
                StarredTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                "${BaseColumns._ID} DESC"
        )

        return ArticleCursorWrapper(cursor)
    }

    //    fun loadAll(): List<ChannelItem> {
//
//        val items = mutableListOf<ChannelItem>()
//        val cursor = queryHistory(null, null)
//
//        cursor.use {
//            it.moveToNext()
//
//            while (!it.isAfterLast) {
//                items.addHistory(it.loadItem())
//                it.moveToNext()
//            }
//        }
//
//        return items
//    }

    companion object {
        private const val TAG = "ArticleStore"

        private var instance: ArticleStore? = null

        fun getInstance(context: Context): ArticleStore {
            if (instance == null) {
                instance = ArticleStore(context)
            }

            return instance!!
        }

        private fun contentValues(item: ChannelItem): ContentValues {
            return ContentValues().apply {
                put(HistoryTable.Cols.ID, item.id)
                put(HistoryTable.Cols.TYPE, item.type)
                put(HistoryTable.Cols.TITLE, item.headline)
                put(HistoryTable.Cols.STANDFIRST, item.standfirst)
            }
        }
    }
}

