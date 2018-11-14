package com.ft.ftchinese.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import android.provider.BaseColumns
import com.ft.ftchinese.models.ChannelItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.db.*
import org.jetbrains.anko.info

class ArticleStore private constructor(context: Context) : AnkoLogger {

    private val mDbHelper = ArticleDbHelper.getInstance(context)

    fun addHistory(item: ChannelItem?) {
        if (item == null) return

        val values = contentValues(item)

        info("Add a reading history: $values")

        try {
            // call getWritableDatabase in background.
            // This also might throw SQLiteException.
            mDbHelper.use {
                insert(HistoryTable.NAME, null, values)
            }
        } catch (e: Exception) {
            info("Error inserting $item: $e")
        }
    }

    fun queryHistory(): ArticleCursorWrapper? {

        val cursor = try {
            mDbHelper.readableDatabase
                    .query(
                            HistoryTable.NAME,
                            arrayOf(Cols.ID,
                                    Cols.TYPE,
                                    Cols.SUB_TYPE,
                                    Cols.TITLE,
                                    Cols.STANDFIRST,
                                    Cols.AUDIO_URL,
                                    Cols.RADIO_URL,
                                    Cols.PUBLISHED_AT),
                            null,
                            null,
                            null,
                            null,
                            "${BaseColumns._ID} DESC"
                    )
        } catch (e: SQLiteException) {
            return null
        }

        return ArticleCursorWrapper(cursor)
    }

    fun historyQuery(): Sequence<Map<String, Any?>> {
        return mDbHelper.use {
            select(HistoryTable.NAME)
                    .column(Cols.ID)
                    .column(Cols.TYPE)
                    .column(Cols.SUB_TYPE)
                    .column(Cols.TITLE)
                    .column(Cols.STANDFIRST)
                    .column(Cols.AUDIO_URL)
                    .column(Cols.RADIO_URL)
                    .column(Cols.PUBLISHED_AT)
                    .column(Cols.READ_AT)
                    .orderBy(BaseColumns._ID, SqlOrderDirection.DESC)
                    .exec {
                        asMapSequence()
                    }
        }
    }

    fun countHistory(): Int {
        return mDbHelper.use {
            val cursor = queryHistory()
            val count = cursor?.count ?: 0
            cursor?.close()
            count
        }
    }

    suspend fun truncateHistory(): Boolean {
        val ok = GlobalScope.async {
            mDbHelper.use {
                try {
                    dropTable(HistoryTable.NAME, true)
                    execSQL(HistoryTable.create)

                    true
                } catch (e: SQLiteException) {
                    info("Cannot truncated reading_history: $e")

                    false
                }
            }
        }

        return ok.await()
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

        return try {
            mDbHelper.use {
                insert(StarredTable.NAME, null, values)
            }
        } catch (e: SQLiteException) {
            info("Error inserting a starred article")
            -1
        }
//        return  mDatabase.insert(StarredTable.NAME, null, values)
    }

    /**
     * @return the number of rows affected if a whereClause is passed in, 0
     * otherwise.
     */
    fun deleteStarred(item: ChannelItem?): Int {
        if (item == null) return 0

        val id = item.id
        return try {
            mDbHelper.use {
                delete(StarredTable.NAME, "${Cols.ID} = ?", arrayOf(id))
            }
        } catch (e: SQLiteException) {
            info("Error deleting starred: $e")

            0
        }
    }

    fun isStarring(item: ChannelItem?): Boolean {

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

        return try {
            mDbHelper.use {
                val cursor = rawQuery(query, arrayOf(id))
                cursor.moveToFirst()

                val exists = cursor.getInt(0)
                cursor.close()

                info("Checking if is starring article $item: $exists")

                exists == 1
            }
        } catch (e: SQLiteException) {
            info("Error when checking is starring an article: $e")

            false
        }
    }

    fun queryStarred(): ArticleCursorWrapper? {
        val cursor = try {
            mDbHelper.readableDatabase
                    .query(
                            HistoryTable.NAME,
                            arrayOf(Cols.ID,
                                    Cols.TYPE,
                                    Cols.SUB_TYPE,
                                    Cols.TITLE,
                                    Cols.STANDFIRST,
                                    Cols.AUDIO_URL,
                                    Cols.RADIO_URL,
                                    Cols.PUBLISHED_AT),
                            null,
                            null,
                            null,
                            null,
                            "${BaseColumns._ID} DESC"
                    )
        } catch (e: SQLiteException) {
            return null
        }

        return ArticleCursorWrapper(cursor)
    }

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

