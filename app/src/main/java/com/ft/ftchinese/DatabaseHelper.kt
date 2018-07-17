package com.ft.ftchinese

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast

class DatabaseHelper(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    private fun updateDatabase(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) {
            db?.execSQL(CREATE_STORY)
            Toast.makeText(context, "Created db", Toast.LENGTH_SHORT).show()

            insertStory(db, "001078513", "中国总理李克强在与欧洲领导人会晤后表示，在努力维护全球贸易体系之际，中国既不愿将美国，也不愿将俄罗斯排除在外。")
        }

        if (oldVersion < 2) {

        }
    }

    companion object {
        private const val DB_NAME = "article"
        private const val DB_VERSION = 1
        private const val TABLE_STORY = "story"

        private val CREATE_STORY = """
        CREATE TABLE IF NOT EXISTS story (
            _id INTEGER PRIMARY KEY AUTOINCREMENT,
            id TEXT NOT NULL UNIQUE ON CONFLICT REPLACE,
            body TEXT,
        );
        """.trimIndent()

        private fun insertStory(db: SQLiteDatabase?, id: String, body: String) {
            val storyValues = ContentValues()
            storyValues.put("id", id)
            storyValues.put("body", body)
            db?.insert(TABLE_STORY, null, storyValues)
        }

        private fun updateStory(db: SQLiteDatabase?, id: String, body: String) {

        }

        // Delete all rows in a table
        private fun deleteAll(db: SQLiteDatabase?, table: String) {
            db?.delete(table, null, null)
        }
    }
}