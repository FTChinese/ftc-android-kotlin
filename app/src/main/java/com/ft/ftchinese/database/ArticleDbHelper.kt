package com.ft.ftchinese.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ArticleDbHelper private constructor(ctx: Context) : SQLiteOpenHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(HistoryTable.SQL_CREATE_TABLE)
        db?.execSQL(StarredTable.SQL_CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(HistoryTable.SQL_DELETE_TABLE)
        db?.execSQL(StarredTable.SQL_DELETE_TABLE)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        const val DATABASE_VERSION = 5
        const val DATABASE_NAME = "article.db"

        private var instance: ArticleDbHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): ArticleDbHelper {
            if (instance == null) {
                instance = ArticleDbHelper(ctx.applicationContext)
            }

            return instance!!
        }
    }
}