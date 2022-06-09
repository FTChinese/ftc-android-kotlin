package com.ft.ftchinese.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Entity(
    tableName = "keyword_history",
    indices = [
        Index(value = ["keyword"], unique = true),
        Index(value = ["modified_at"])
    ]
)
data class KeywordEntry(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    @ColumnInfo
    val keyword: String,

    @ColumnInfo(name = "modified_at")
    val modifierAt: Long
) {
    companion object {
        @JvmStatic
        fun newInstance(keyword: String) = KeywordEntry(
            keyword = keyword,
            modifierAt = System.currentTimeMillis() / 1000
        )
    }
}

@Dao
interface KeywordHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOne(entry: KeywordEntry)

    @Query("Select * FROM keyword_history ORDER BY modified_at DESC LIMIT 10")
    fun getAll(): List<KeywordEntry>

    @Query("DELETE FROM keyword_history")
    fun deleteAll()

    @RawQuery
    fun vacuumDb(supportSQLiteQuery: SupportSQLiteQuery): Int
}

@Database(
    entities = [
        KeywordEntry::class
    ],
    version = 2
)
abstract class SearchDb : RoomDatabase() {
    abstract fun keywordHistoryDao(): KeywordHistoryDao

    companion object {
        private var instance: SearchDb? = null

        @Synchronized
        fun getInstance(context: Context): SearchDb {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    SearchDb::class.java,
                    "search.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }

            return instance!!
        }
    }
}
