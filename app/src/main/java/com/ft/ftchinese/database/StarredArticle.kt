package com.ft.ftchinese.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.models.Permission
import com.ft.ftchinese.models.Tier
import java.util.*


@Entity(
        tableName = "starred_article",
        indices = [
            Index(value = ["id", "type"], unique = true)
        ]
)
data class StarredArticle(
        @PrimaryKey(autoGenerate = true)
        val _id: Int = 0,

        @ColumnInfo val
        id: String,

        @ColumnInfo
        val type: String,

        @ColumnInfo(name = "sub_type")
        val subType: String = "",

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
        val publishedAt: String = "",

        @ColumnInfo(name = "starred_at")
        var starredAt: String = "",

        // Indicates which tier of membership could
        // access this article.
        // Empty means free user.
        @ColumnInfo(name = "tier")
        var tier: String = "", // "", standard, premium

        @ColumnInfo(name = "web_url")
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
                tag = keywords,
                webUrl = webUrl,
                isWebpage = isWebpage
        )
    }

    private fun requireMembership(): Boolean {
        return (tier == Tier.STANDARD.string()) || (tier == Tier.PREMIUM.string())
    }

    fun permission(): Permission {
        return when {
            tier == Tier.STANDARD.string() -> Permission.STANDARD
            tier == Tier.PREMIUM.string() -> Permission.PREMIUM
            isSevenDaysOld() -> Permission.STANDARD
            else -> Permission.FREE
        }
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
interface StarredArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOne(article: StarredArticle)

    @Query("DELETE FROM starred_article WHERE id = :id AND type = :type")
    fun delete(id: String, type: String)

    @Query("SELECT * FROM starred_article ORDER BY _id DESC")
    fun getAll(): LiveData<List<StarredArticle>>

    @Query("SELECT EXISTS(SELECT * FROM starred_article WHERE id = :id AND type = :type) AS found")
    fun exists(id: String, type: String): Boolean
}