package com.ft.ftchinese.models

import android.text.format.DateFormat
import com.beust.klaxon.Json
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.util.formatSQLDateTime
import org.jetbrains.anko.AnkoLogger
import org.threeten.bp.LocalDateTime
import java.util.*

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

        // 0 - free
        // 1 - standard
        // 2 - premium
        @Json(name = "accessright")
        val accesibleBy: String,

        @Json(name = "last_publish_time")
        val publishedAt: String,

        @Json(name = "story_pic")
        val cover: StoryPic,

        @Json(name = "relative_story")
        val relatedStory: List<RelatedStory>
) : AnkoLogger {

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
                tier = requireMemberTier()?.string() ?: "",
                webUrl = channelItem?.getCanonicalUrl() ?: ""
        )
    }

    private fun requireMemberTier(): Tier? {
        return when (accesibleBy) {
            "0" -> null
            "1" -> Tier.STANDARD
            "2" -> Tier.PREMIUM
            else -> null
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
        return (requireMemberTier() == null) && !isSevenDaysOld()
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
                tier = requireMemberTier()?.string() ?: "",
                webUrl = channelItem?.getCanonicalUrl() ?: ""
        )
    }

    val isBilingual: Boolean
        get() = bodyEN.isNotBlank()

    val byline: String
        get() = "$orgCN $authorCN $locationCN"

    private val tags: List<String>
        get() = tag.split(",")

    // This is a very weired way to product a comma-separated string. Why not use array and join directly?
    // And why a string? Isn't it keeping `keywords` as an array of string more ideal?
    val keywords: String
        get() = "$tag,$area,$topic,$genre"
                .replace(Regex(",+"), ",")
                .replace(Regex("^,"), "")
                .replace(Regex(",$"), "")

    fun getAdZone(homepageZone: String, fallbackZone: String, overrideAdZone: String): String {
        if (keywords.isBlank()) {
            return fallbackZone
        }

        for (sponsor in SponsorManager.sponsors) {
            if (sponsor.tag.isBlank()) {
                continue
            }

            val matched = isMatchInKeysAndBody(sponsor)

            if ((keywords.contains(sponsor.tag) || keywords.contains(sponsor.title) || matched) && sponsor.zone.isNotEmpty() ) {
                return if (sponsor.zone.contains("/")) {
                    sponsor.zone
                } else {
                    "home/special/${sponsor.zone}"
                }
            }
        }

        if (overrideAdZone != homepageZone) {
            return overrideAdZone
        }

        val regex = Regex("lifestyle|management|opinion|创新经济|markets|economy|china")

        val result = regex.find(keywords)

        if (result != null) {
            return result.value
        }

        return fallbackZone
    }

    fun getAdTopic(): String {
        if (keywords.isBlank()) {
            return ""
        }

        for (sponsor in SponsorManager.sponsors) {
            val matched = isMatchInKeysAndBody(sponsor)

            if (matched && sponsor.cntopic != "") {
                return sponsor.cntopic
            }
        }

        return ""
    }

    fun isMatchInKeysAndBody(sponsor: Sponsor): Boolean {
        if (sponsor.storyKeyWords.isBlank()) {
            return false
        }

        val regexStr = sponsor.storyKeyWords.replace(Regex(", *"), "|")
        val regex = Regex(regexStr)

        val fullContent = "$bodyCN$keywords$titleCN$titleEN$bodyEN"

        return regex.containsMatchIn(fullContent)
    }

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
    fun htmlForTheme(sponsorTitle: String?): String {
        val firstTag = sponsorTitle ?: tags[0]

        return """
            <div class="story-theme">
                <a target="_blank" href="/tag/$firstTag">${sponsorTitle ?: firstTag}</a>
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
        if (bodyEN.isBlank()) {
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
            "${it.en}${it.cn}"
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