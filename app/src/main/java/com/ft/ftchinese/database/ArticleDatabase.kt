package com.ft.ftchinese.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.ft.ftchinese.models.ChannelItem

@Database(entities = [ChannelItem::class], version = 1)
abstract class ArticleDatabase : RoomDatabase() {
    abstract fun readingHistory(): ReadingHistory

    companion object {
        private var INSTANCE: ArticleDatabase? = null

        fun getInstance(context: Context): ArticleDatabase? {
            if (INSTANCE == null) {
                synchronized(ArticleDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext, ArticleDatabase::class.java, "article.db")
                            .build()
                }
            }

            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}