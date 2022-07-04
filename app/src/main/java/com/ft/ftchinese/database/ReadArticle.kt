package com.ft.ftchinese.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.content.OpenGraphMeta
import com.ft.ftchinese.model.content.Story
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.formatSQLDateTime
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.repository.HostConfig
import org.threeten.bp.LocalDateTime
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
) {

    val canonicalUrl: String
        get() = "https://${HostConfig.HOST_FTC}/$type/$id"

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
        )
    }

    fun toStarred(): StarredArticle {
        return StarredArticle(
            id = id,
            type = type,
            subType = subType,
            title = title,
            standfirst = standfirst,
            keywords = keywords,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            radioUrl = radioUrl,
            publishedAt = publishedAt,
            starredAt = formatSQLDateTime(LocalDateTime.now()),
            tier = tier,
        )
    }

    fun permission(): Permission {
        return when {
            tier == Tier.STANDARD.toString() -> Permission.STANDARD
            tier == Tier.PREMIUM.toString() -> Permission.PREMIUM
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

    companion object {
        /**
         * After loaded JSON data for a story.
         */
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
            )
        }

        @JvmStatic
        fun fromTeaser(teaser: Teaser): ReadArticle {
            return ReadArticle(
                id = teaser.id,
                type = teaser.type.toString(),
                subType = teaser.subType ?: "",
                title = teaser.title,
                standfirst = "",
                keywords = teaser.tag,
                imageUrl = "",
                audioUrl = teaser.audioUrl ?: "",
                radioUrl = teaser.radioUrl ?: "",
                publishedAt = teaser.publishedAt ?: "",
                readAt = formatSQLDateTime(LocalDateTime.now()),
                tier = "",
            )
        }

        /**
         * For articles without JSON api, we need to get
         * structured data from JS.
         */
        @JvmStatic
        fun fromOpenGraph(og: OpenGraphMeta, teaser: Teaser?): ReadArticle {
            return ReadArticle(
                id = if (teaser?.id.isNullOrBlank()) {
                    og.extractId()
                } else {
                    teaser?.id
                } ?: "",
                type = if (teaser?.type == null) {
                    og.extractType()
                } else {
                    teaser.type.toString()
                } ,
                subType = teaser?.subType ?: "",
                title = if (teaser?.title.isNullOrBlank()) {
                    og.title
                } else {
                    teaser?.title
                } ?: "",
                standfirst = og.description,
                keywords = teaser?.tag ?: og.keywords,
                imageUrl = og.image,
                audioUrl = teaser?.audioUrl ?: "",
                radioUrl = teaser?.radioUrl ?: "",
                publishedAt = "",
                tier =  when {
                    og.keywords.contains("会员专享") -> Tier.STANDARD.toString()
                    og.keywords.contains("高端专享") -> Tier.PREMIUM.toString()
                    else -> ""
                },
                readAt = formatSQLDateTime(LocalDateTime.now()),
            )
        }
    }
}

@Dao
interface ReadingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOne(article: ReadArticle)

    @Query("SELECT * FROM reading_history ORDER BY _id DESC")
    fun getAll(): LiveData<List<ReadArticle>>

    @Query("SELECT * FROM reading_history ORDER BY _id DESC")
    fun loadAll(): List<ReadArticle>

    @Query("SELECT * FROM reading_history WHERE id = :id AND type = :type")
    fun getOne(id: String, type: String): ReadArticle?

    @Query("SELECT COUNT(id) FROM reading_history")
    fun count(): Int

    @Query("DELETE FROM reading_history")
    fun deleteAll()

    @RawQuery
    fun vacuumDb(supportSQLiteQuery: SupportSQLiteQuery): Int
}
