package com.ft.ftchinese.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val readTable = "reading_history"

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `starred_article` (
                `_id`           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `id`            TEXT NOT NULL,
                `type`          TEXT NOT NULL,
                `sub_type`      TEXT NOT NULL,
                `title`         TEXT NOT NULL,
                `standfirst`    TEXT NOT NULL,
                `keywords`      TEXT NOT NULL,
                `image_url`     TEXT NOT NULL,
                `audio_url`     TEXT NOT NULL,
                `radio_url`     TEXT NOT NULL,
                `published_at`  TEXT NOT NULL,
                `starred_at`    TEXT NOT NULL,
                `canonical_url` TEXT NOT NULL,
                `is_webpage`    INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            INSERT INTO new_starred_article (
                id,
                type,
                sub_type,
                title,
                standfirst,
                keywords,
                image_url,
                audio_url,
                radio_url,
                published_at,
                starred_at,
                canonical_url,
                is_webpage
            )
            SELECT id,
                type,
                IFNULL(sub_type, ''),
                IFNULL(title, ''),
                IFNULL(standfirst, ''),
                '',
                '',
                IFNULL(audio_url, ''),
                IFNULL(radio_url, ''),
                IFNULL(published_at, ''),
                IFNULL(read_at, ''),
                '',
                0
            FROM starred_article
        """.trimIndent())

        database.execSQL("DROP TABLE starred_article")

        database.execSQL("ALTER TABLE new_starred_article RENAME TO starred_article")
    }
}

@Database(
        entities = [
            StarredArticle::class,
            ReadArticle::class
        ],
        version = 8
)
abstract class ArticleDb : RoomDatabase() {
    abstract fun starredDao(): StarredArticleDao
    abstract fun readDao(): ReadingHistoryDao

    companion object {
        private var instance: ArticleDb? = null

        @Synchronized
        fun getInstance(context: Context): ArticleDb {
            if (instance == null) {
                instance = Room.databaseBuilder(
                        context.applicationContext,
                        ArticleDb::class.java,
                        "article.db")
                        .addMigrations(
                                MIGRATION_7_8)
                        .fallbackToDestructiveMigration()
                        .build()
            }

            return instance!!
        }
    }
}
