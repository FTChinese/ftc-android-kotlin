package com.ft.ftchinese.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Entity(
    tableName = "search_entry",
    indices = [
        Index(value = ["keyword"], unique = true)
    ]
)
data class SearchEntry(
    @PrimaryKey(autoGenerate = true)
    val _id: Int = 0,

    @ColumnInfo
    val keyword: String,
)

@Dao
interface SearchEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOne(entry: SearchEntry)

    @Query("Select * FROM search_entry ORDER BY _id DESC LIMIT 10")
    fun getAll(): List<SearchEntry>

    @Query("DELETE FROM search_entry")
    fun deleteAll()

    @RawQuery
    fun vacuumDb(supportSQLiteQuery: SupportSQLiteQuery): Int
}

@Database(
    entities = [
        SearchEntry::class
    ],
    version = 1
)
abstract class SearchDb : RoomDatabase() {
    abstract fun searchEntryDao(): SearchEntryDao

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
