package com.ft.ftchinese.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.Story
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.fetch.formatSQLDateTime
import org.threeten.bp.LocalDateTime

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

    fun toTeaser(): Teaser {
        return Teaser(
            id = id,
            type = ArticleType.fromString(type),
            subType = subType,
            title = title,
            audioUrl = audioUrl,
            radioUrl = radioUrl,
            publishedAt = publishedAt,
            tag = keywords,
            webUrl = webUrl
        )
    }

    companion object {
        @JvmStatic
        fun fromStory(story: Story): ReadArticle {
            return ReadArticle(
                id = story.id,
                type = story.teaser?.type?.toString() ?: "",
                subType = story.teaser?.subType ?: "",
                title = story.titleCN,
                standfirst = story.standfirstCN,
                keywords = story.keywords,
                imageUrl = story.cover.smallbutton,
                audioUrl = story.teaser?.audioUrl ?: "",
                radioUrl = story.teaser?.radioUrl ?: "",
                publishedAt = story.publishedAt,
                readAt = formatSQLDateTime(LocalDateTime.now()),
                tier = story.requireMemberTier()?.toString() ?: "",
                webUrl = story.teaser?.getCanonicalUrl() ?: "",
            )
        }
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
