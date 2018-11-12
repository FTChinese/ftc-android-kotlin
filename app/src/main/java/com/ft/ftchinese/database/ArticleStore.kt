package com.ft.ftchinese.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.ft.ftchinese.models.ChannelItem
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ArticleStore private constructor(context: Context) : AnkoLogger {

    private val mDatabase: SQLiteDatabase = ArticleDbHelper.getInstance(context)
            .writableDatabase

    fun addHistory(item: ChannelItem?): Long {
        if (item == null) return 0

        val values = contentValues(item)

        info("Add a reading history: $values")

        return mDatabase.insert(HistoryTable.NAME, null, values)
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

    fun countHistory(): Int {
        val cursor = queryHistory()
        val count = cursor.count
        cursor.close()
        return count
    }

    fun dropHistory() {
        try {
            mDatabase.execSQL(HistoryTable.delete)
            mDatabase.execSQL(HistoryTable.delete)
        } catch (e: Exception) {
            info("Drop reading_history table error: $e")
        }
    }

    /**
     * Return -1 for error.
     * 0 for noop.
     * Return row id if ok.
     */
    fun addStarred(item: ChannelItem?): Long {
        if (item == null) return 0

        val values = contentValues(item)

        info("Add a starred article: $values")

        return  mDatabase.insert(StarredTable.NAME, null, values)
    }

    fun deleteStarred(item: ChannelItem?): Int {
        if (item == null) return 0

        val id = item.id
        return mDatabase.delete(StarredTable.NAME, "${Cols.ID} = ?", arrayOf(id))
    }

    fun isStarring(item: ChannelItem?): Boolean {
        info("Checking if is starring article $item")
        if (item == null) return false
        val id = item.id
        val query = """
            SELECT EXISTS(
                SELECT *
                FROM ${StarredTable.NAME}
                WHERE ${Cols.ID} = ?
                LIMIT 1
            )
        """.trimIndent()

        val cursor = mDatabase.rawQuery(query, arrayOf(id))

        cursor.moveToFirst()
        val exists = cursor.getInt(0)

        info("Is starring $exists")
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

        private var instance: ArticleStore? = null

        fun getInstance(context: Context): ArticleStore {
            if (instance == null) {
                instance = ArticleStore(context)
            }

            return instance!!
        }

        private fun contentValues(item: ChannelItem): ContentValues {
            return ContentValues().apply {
                put(Cols.ID, item.id)
                put(Cols.TYPE, item.type)
                put(Cols.SUB_TYPE, item.subType)
                put(Cols.TITLE, item.headline)
                put(Cols.STANDFIRST, item.standfirst)
                put(Cols.AUDIO_URL, item.eaudio)
                put(Cols.RADIO_URL, item.shortlead)
                put(Cols.PUBLISHED_AT, item.timeStamp)
            }
        }
    }
}

