package com.ft.ftchinese.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StarredArticle::class,
        ReadArticle::class
    ],
    version = 9
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
                    .fallbackToDestructiveMigration()
                    .build()
            }

            return instance!!
        }
    }
}
