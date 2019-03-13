package com.ft.ftchinese.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ft.ftchinese.models.ChannelItem


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
                webUrl = webUrl,
                isWebpage = isWebpage
        )
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