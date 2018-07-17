package com.ft.ftchinese

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast

/**
 * try {
    db = dbHelper?.readableDatabase

    val retrieveResult = async { DatabaseHelper.queryStory(db, item.id) }

    val storyData = retrieveResult.await()

    if (storyData != null) {
    webview.loadDataWithBaseURL("http://www.ftchinese.com", storyData.body, "text/html", null, null)
    stopProgress()

    db?.close()
    return@launch
    }

    } catch (e: SQLiteException) {
    Log.e(TAG, e.toString())
    }
    async { DatabaseHelper.insertStory(db, item.id, html) }
 */
class DatabaseHelper(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        updateDatabase(db, 0, DB_VERSION)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        updateDatabase(db, oldVersion, newVersion)
    }

    private fun updateDatabase(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) {
            db?.execSQL(CREATE_STORY)
            Toast.makeText(context, "Created db", Toast.LENGTH_SHORT).show()

            Log.i(TAG, "Created database $DB_VERSION")
        }

        if (oldVersion < 2) {

        }
    }

    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DB_NAME = "article"
        private const val DB_VERSION = 1
        private const val TABLE_STORY = "story"
        private var INSTANCE: DatabaseHelper? = null

        private val CREATE_STORY = """
        CREATE TABLE IF NOT EXISTS story (
            _id INTEGER PRIMARY KEY AUTOINCREMENT,
            id TEXT NOT NULL UNIQUE ON CONFLICT REPLACE,
            body TEXT
        );
        """.trimIndent()

        fun getInstance(context: Context): DatabaseHelper? {
            if (INSTANCE == null) {
                synchronized(DatabaseHelper::class) {
                    INSTANCE = DatabaseHelper(context)
                }
            }
            return INSTANCE
        }

        fun insertStory(db: SQLiteDatabase?, id: String, body: String) {
            val storyValues = ContentValues()
            storyValues.put("id", id)
            storyValues.put("body", body)
            db?.insert(TABLE_STORY, null, storyValues)
        }

        fun queryStory(db: SQLiteDatabase?, id: String): StoryData? {
            val cursor = db?.query(TABLE_STORY, arrayOf("id", "body"), "id = ?", arrayOf(id), null, null, null, "1")

            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getString(0)
                val body = cursor.getString(1)
                cursor.close()
                return StoryData(id, body)
            }
            return null
        }

        fun updateStory(db: SQLiteDatabase?, id: String, body: String) {
            val storyValues = ContentValues()
            storyValues.put("body", body)
            db?.update(TABLE_STORY, storyValues, "id = ?", arrayOf(id))
        }

        // Delete all rows in a table
        fun deleteAll(db: SQLiteDatabase?, table: String) {
            db?.delete(table, null, null)
        }
    }
}