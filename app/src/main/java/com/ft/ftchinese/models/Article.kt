package com.ft.ftchinese.models

import android.text.format.DateFormat
import com.beust.klaxon.Json
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.util.formatSQLDateTime
import org.threeten.bp.LocalDateTime

data class Bilingual(
        var cn: String,
        var en: String?
)

data class StoryPic(
        val smallbutton: String,
        val other: String
)

data class RelatedStory(
        val id: String,

        @Json(name = "cheadline")
        val titleCN: String,

        @Json(name = "eheadline")
        val titleEN: String = "", // This might not exist.

        @Json(name = "last_publish_time")
        val publishedAt: String
)

class Story (
        val id: String = "",

        @Json(name = "fileupdatetime")
        val createdAt: String,

        // Used as share's title
        @Json(name = "cheadline")
        val titleCN: String,

        @Json(name = "clongleadbody")
        val standfirstCN: String,

        @Json(name = "cbody")
        val bodyCN: String,

        @Json(name = "eheadline")
        val titleEN: String = "",

        @Json(name = "elongleadbody")
        val standfirstEN: String = "",

        @Json(name = "ebody")
        val bodyEN: String = "",

        @Json(name = "ebyline_description")
        val orgEN: String = "",

        @Json(name = "cbyline_description")
        val orgCN: String,

        @Json(name = "cauthor")
        val authorCN: String,

        @Json(name = "cbyline_status")
        val locationCN: String,

        @Json(name = "eauthor")
        val authorEN: String = "",

        @Json(name = "ebyline_status")
        val locationEN: String = "",

        val tag: String,
        val genre: String,
        val topic: String,
        val industry: String,
        val area: String,

        @Json(name = "last_publish_time")
        val publishedAt: String,

        @Json(name = "story_pic")
        val cover: StoryPic,

        @Json(name = "relative_story")
        val relatedStory: List<RelatedStory>
) {

    fun toStarredArticle(channelItem: ChannelItem?): StarredArticle {
        return StarredArticle(
                id = id,
                type = channelItem?.type ?: "",
                subType = channelItem?.subType ?: "",
                title = titleCN,
                standfirst = standfirstCN,
                keywords = keywords,
                imageUrl = cover.smallbutton,
                audioUrl = channelItem?.audioUrl ?: "",
                radioUrl = channelItem?.radioUrl ?: "",
                publishedAt = publishedAt,
                webUrl = channelItem?.getCanonicalUrl() ?: ""
        )
    }

    fun toReadArticle(channelItem: ChannelItem?): ReadArticle {
        return ReadArticle(
                id = id,
                type = channelItem?.type ?: "",
                subType = channelItem?.subType ?: "",
                title = titleCN,
                standfirst = standfirstCN,
                keywords = keywords,
                imageUrl = cover.smallbutton,
                audioUrl = channelItem?.audioUrl ?: "",
                radioUrl = channelItem?.radioUrl ?: "",
                publishedAt = publishedAt,
                readAt = formatSQLDateTime(LocalDateTime.now()),
                webUrl = channelItem?.getCanonicalUrl() ?: ""
        )
    }

    val isBilingual: Boolean
        get() = bodyEN.isNotBlank()

    val byline: String
        get() = "$orgCN $authorCN $locationCN"

    private val tags: List<String>
        get() = tag.split(",")

    val keywords: String
        get() = "$tag,$area,$topic,$genre"
                .replace(Regex(",+"), ",")
                .replace(Regex("^,"), "")
                .replace(Regex(",$"), "")

    fun formatPublishTime(): String {
        return DateFormat.format("yyyy年M月d日 HH:mm", publishedAt.toLong() * 1000) as String
    }

    // HTML for the {related-stories}
    fun htmlForRelatedStories(): String {

        val listItems = relatedStory.mapIndexed { index, relatedStory ->
            """
            <li class="mp${index+1}"><a target="_blank" href="/story/${relatedStory.id}\">${relatedStory.titleCN}</a></li>
            """.trimIndent()
        }
                .joinToString("")

        return """
            <div class="story-box">
                <h2 class="box-title"><a>相关文章</a></h2>
                <ul class="top10">$listItems</ul>
            </div>
        """.trimIndent()
    }

    // HTML for the {related-topics}
    fun htmlForRelatedTopics(): String {
        return tags.mapIndexed { index, s ->
            """
            <li class="story-theme mp${index+1}">
                <a target="_blank" href="/tag/$s\">$s</a>
                <div class="icon-right">
                    <button class="myft-follow plus" data-tag="$s" data-type="tag">关注</button>
                </div>
            </li>
            """.trimIndent()
        }
                .joinToString("")

    }

    fun htmlForCoverImage(): String {
        return """
            <div class="story-image image" style="margin-bottom:0;">
                <figure data-webUrl="${cover.smallbutton}" class="loading"></figure>
            </div>
        """.trimIndent()
    }

    // HTML for {story-theme}
    fun htmlForTheme(): String {
        val firstTag = tags[0]

        return """
            <div class="story-theme">
                <a target="_blank" href="/tag/$firstTag">$firstTag</a>
                <button class="myft-follow plus" data-tag="$firstTag" data-type="tag">关注</button>
            </div>
        """.trimIndent()
    }

    fun getCnBody(withAd: Boolean = true): String {
        if (!withAd) {
            return bodyCN
        }

        val arrBody = splitCnBody().toMutableList()

        // Insert ad after paragraph 3 and paragraph 9 of the original list.
        if (arrBody.size > 3) {
            arrBody.add(3, AdParser.getAdCode(AdPosition.MIDDLE_ONE))
        } else {
            arrBody.add(arrBody.size, AdParser.getAdCode(AdPosition.MIDDLE_ONE))
        }

        if (arrBody.size > 10) {
            arrBody.add(10, AdParser.getAdCode(AdPosition.MIDDLE_TWO))
        }

        return arrBody.joinToString("")
    }

    fun getEnBody(withAd: Boolean = true): String {
        if (bodyEN.isNullOrBlank()) {
            return ""
        }

        if (!withAd) {
            return bodyEN
        }

        val arrBody = splitEnBody().toMutableList()

        // Insert ad after paragraph 3 and paragraph 9 of the original list.
        if (arrBody.size > 3) {
            arrBody.add(3, AdParser.getAdCode(AdPosition.MIDDLE_ONE))
        } else {
            arrBody.add(arrBody.size, AdParser.getAdCode(AdPosition.MIDDLE_ONE))
        }

        if (arrBody.size > 10) {
            arrBody.add(10, AdParser.getAdCode(AdPosition.MIDDLE_TWO))
        }

        return arrBody.joinToString("")
    }

    fun getBilingualBody(): String {
        if (bodyEN.isBlank()) {
            return ""
        }

        val alignedBody = alignBody()

        return alignedBody.joinToString("") {
            "${it.cn}${it.en}"
        }
    }

    private fun splitCnBody(): List<String> {
        return bodyCN.split("\r\n")
    }

    private fun splitEnBody(): List<String> {
        return bodyEN.split("\r\n")
    }

    /**
     * Align english text to chinese text paragraph by paragraph.
     */
    private fun alignBody(): List<Bilingual> {
        val cnArray = bodyCN.split("\r\n")
        val enArray = bodyEN.split("\r\n")

        val bi = mutableListOf<Bilingual>()

        var cIndex = 0
        var eIndex = 0

        val cLen = cnArray.size
        val eLen = enArray.size


        while (cIndex < cLen && eIndex < eLen) {
            val pair = Bilingual(cn = cnArray[cIndex], en = enArray[eIndex])
            bi.add(pair)
            cIndex++
            eIndex++
        }

        while (cIndex < cLen) {
            val pair = Bilingual(cn = cnArray[cIndex], en = "")
            bi.add(pair)
            cIndex++
        }

        while (eIndex < eLen) {
            val pair = Bilingual(cn = "", en = enArray[eIndex])
            bi.add(pair)
            eIndex++
        }

        return bi
    }
}