package com.ft.ftchinese.database

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast

class ArticleBaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        try {
            db?.execSQL(CREATE_READING_HISTORY)
            Toast.makeText(context, "Article db created", Toast.LENGTH_SHORT).show()
        } catch (e: SQLException) {
            Toast.makeText(context, "Cannot create database: $e", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private const val VERSION = 1
        private const val DATABASE_NAME = "article.db"
        private val CREATE_READING_HISTORY = """
            CREATE TABLE IF NOT EXISTS ${ReadingHistory.TABLE_NAME} (
                ${ReadingHistory.COL_ID} TEXT,
                ${ReadingHistory.COL_TYPE} TEXT,
                ${ReadingHistory.COL_SUB_TYPE} TEXT,
                ${ReadingHistory.COL_TITLE} TEXT,
                ${ReadingHistory.COL_STANDFIRST} TEXT,
                read_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (id, type) ON CONFLICT IGNORE)
        """.trimIndent()
    }
}