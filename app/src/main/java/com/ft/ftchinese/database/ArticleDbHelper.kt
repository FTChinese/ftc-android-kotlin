package com.ft.ftchinese.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * Create and manager database.
 * You must override the `onCreate()` and `onUpgrade()` methods.
 */
class ArticleDbHelper private constructor(ctx: Context) : SQLiteOpenHelper(ctx, DB_NAME, null, DB_VERSION), AnkoLogger {

    /**
     * Gets called when the database first gets created on the device.
     */
    override fun onCreate(db: SQLiteDatabase?) {
        updateDatabase(db, 0, DB_VERSION)
    }

    /**
     * Gets called when the database needs to be upgraded.
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        info("Database upgrade from $oldVersion to $newVersion")

        updateDatabase(db, oldVersion, newVersion)
    }

    private fun updateDatabase(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) {
            info("Create table: ${HistoryTable.create}")
            db?.execSQL(HistoryTable.create)
            info("Create table: ${StarredTable.create}")
            db?.execSQL(StarredTable.create)
        }

        // From version 1.0.0 to version 1.0.1
        if (oldVersion == 6 && newVersion == 7) {
            info("Update db schema from version 6 to version 7")
            updateV6(db, StarredTable.NAME)
            updateV6(db, HistoryTable.NAME)
        }
    }

    private fun updateV6(db: SQLiteDatabase?, tableName: String) {
        db?.execSQL("""
            ALTER TABLE $tableName
            RENAME COLUMN tier to ${Cols.TYPE}
        """.trimIndent())
        db?.execSQL("""
            ALTER TABLE $tableName
            ADD COLUMN ${Cols.SUB_TYPE} TEXT
        """.trimIndent())
        db?.execSQL("""
            ALTER TABLE $tableName
            ADD COLUMN ${Cols.AUDIO_URL} TEXT
        """.trimIndent())
        db?.execSQL("""
            ALTER TABLE $tableName
            ADD COLUMN ${Cols.RADIO_URL} TEXT
        """.trimIndent())
        db?.execSQL("""
            ALTER TABLE $tableName
            ADD COLUMN ${Cols.PUBLISHED_AT} TEXT
        """.trimIndent())
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        info("Database downgrade from $oldVersion, $newVersion")
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        // Version 1.0.0 uses version 6
        const val DB_VERSION = 7
        const val DB_NAME = "article.db"

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