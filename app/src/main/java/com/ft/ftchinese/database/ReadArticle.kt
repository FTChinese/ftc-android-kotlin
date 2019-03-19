package com.ft.ftchinese.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.models.Tier
import java.util.*

@Entity(
        tableName = "reading_history",
        indices = [
                Index(value = ["id", "type"], unique = true)
        ]
)
data class ReadArticle(
        @PrimaryKey(autoGenerate = true)
        val _id: Int = 0,

        @ColumnInfo
        val id: String,

        @ColumnInfo
        val type: String,

        @ColumnInfo(name = "sub_type")
        val subType: String,

        @ColumnInfo
        val title: String,

        @ColumnInfo
        val standfirst: String,

        @ColumnInfo
        val keywords: String = "",

        @ColumnInfo(name = "image_url")
        val imageUrl: String = "",

        @ColumnInfo(name = "audio_url")
        val audioUrl: String = "",

        @ColumnInfo(name = "radio_url")
        val radioUrl: String = "",

        @ColumnInfo(name = "published_at")
        val publishedAt: String,

        @ColumnInfo(name = "read_at")
        val readAt: String = "",

        @ColumnInfo(name = "tier")
        var tier: String = "", // "", standard, premium

        @ColumnInfo(name = "canonical_url")
        var webUrl: String = "",

        @ColumnInfo(name = "is_webpage")
        var isWebpage: Boolean = false
) {
    fun toChannelItem(): ChannelItem {
        return ChannelItem(
                id = id,
                type = type,
                subType = subType,
                title = title,
                audioUrl = audioUrl,
                radioUrl = radioUrl,
                publishedAt = publishedAt,
                webUrl = webUrl
        )
    }

    private fun requireMembership(): Boolean {
        return (tier == Tier.STANDARD.string()) || (tier == Tier.PREMIUM.string())
    }

    private fun isSevenDaysOld(): Boolean {
        if (publishedAt.isBlank()) {
            return false
        }

        val sevenDaysLater = Date((publishedAt.toLong() + 7 * 24 * 60 * 60) * 1000)
        val now = Date()

        if (sevenDaysLater.after(now)) {
            return false
        }

        return true
    }

    fun isFree(): Boolean {
        return !requireMembership() && !isSevenDaysOld()
    }
}

@Dao
interface ReadingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOne(article: ReadArticle)

    @Query("SELECT * FROM reading_history ORDER BY _id DESC")
    fun getAll(): LiveData<List<ReadArticle>>

    @Query("SELECT COUNT(id) FROM reading_history")
    fun count(): Int

    @Query("DELETE FROM reading_history")
    fun deleteAll()

    @RawQuery
    fun vacuumDb(supportSQLiteQuery: SupportSQLiteQuery): Int
}