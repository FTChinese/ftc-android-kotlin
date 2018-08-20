package com.ft.ftchinese.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import com.ft.ftchinese.models.ChannelItem

//@Dao
//interface ReadingHistory {
//    @Query("SELECT * FROM reading_history")
//    fun loadAll(): List<ChannelItem>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insertOne(item: ChannelItem)
//}


object ReadingHistory {

    private const val TAG = "ReadingHistory"

    object HistoryEntry : BaseColumns {
        const val TABLE_NAME = "reading_history"
        const val COL_ID = "id"
        const val COL_TYPE = "type"
        const val COL_TITLE = "title"
        const val COL_STANDFIRST = "standfirst"
    }

    val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS ${HistoryEntry.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY,
                ${HistoryEntry.COL_ID} TEXT,
                ${HistoryEntry.COL_TYPE} TEXT,
                ${HistoryEntry.COL_TITLE} TEXT,
                ${HistoryEntry.COL_STANDFIRST} TEXT,
                read_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (id, type) ON CONFLICT REPLACE)
        """.trimIndent()

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${HistoryEntry.TABLE_NAME}"

    var dbHelper: ReadingHistoryDbHelper? = null

    private var db: SQLiteDatabase? = null

    fun insert(item: ChannelItem) {
        val values = getContentValues(item)
        val result = getDb()?.insert(HistoryEntry.TABLE_NAME, null, values)
        Log.i(TAG, "Insert result: $result")
    }

    fun loadAll(): List<ChannelItem>? {
        val cursor = getDb()?.query(
                HistoryEntry.TABLE_NAME,
                arrayOf(HistoryEntry.COL_ID, HistoryEntry.COL_TYPE, HistoryEntry.COL_TITLE, HistoryEntry.COL_STANDFIRST),
                null,
                null,
                null,
                null,
                "${BaseColumns._ID} DESC"

        ) ?: return null

        Log.i(TAG, "Loading ${cursor.count} items")

        val items = mutableListOf<ChannelItem>()


        with(cursor) {
            while (moveToNext()) {
                val id = getString(0)
                val type = getString(1)
                val title = getString(2)
                val standfirst = getString(3)

                val item = ChannelItem(id = id, type = type, headline = title)
                item.standfirst = standfirst

                items.add(item)
            }
        }

        return items
    }

    private fun getContentValues(item: ChannelItem): ContentValues {
        return ContentValues().apply {
            put(HistoryEntry.COL_ID, item.id)
            put(HistoryEntry.COL_TYPE, item.type)
            put(HistoryEntry.COL_TITLE, item.headline)
            put(HistoryEntry.COL_STANDFIRST, item.standfirst)
        }
    }

    private fun getDb(): SQLiteDatabase? {
        if (db == null) {
            db = dbHelper?.writableDatabase
        }
        return db
    }
}

class ReadingHistoryDbHelper(ctx: Context) : SQLiteOpenHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(ReadingHistory.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(ReadingHistory.SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        const val DATABASE_VERSION = 4
        const val DATABASE_NAME = "article.db"

        private var instance: ReadingHistoryDbHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): ReadingHistoryDbHelper? {
            if (instance == null) {
                instance = ReadingHistoryDbHelper(ctx.applicationContext)
            }

            return instance!!
        }
    }
}