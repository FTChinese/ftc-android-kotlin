package com.ft.ftchinese.model.content

import android.text.format.DateFormat
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.tracking.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

data class Bilingual(
    var cn: String,
    var en: String?
)

@Serializable
data class StoryPic(
    val smallbutton: String,
)

@Serializable
data class RelatedStory(
    val id: String,

    @SerialName("cheadline")
    val titleCN: String? = null,

    @SerialName("eheadline")
    val titleEN: String? = null, // This might not exist.

    @SerialName("last_publish_time")
    val publishedAt: String? = null
)

@Serializable
data class AiAudio(
    @SerialName("ai_audio_e")
    val english: String = "",
    @SerialName("ai_audio_c")
    val chinese: String = "",
    @SerialName("interactive_id")
    val interactiveId: String = "",
)


@Serializable
data class ColumnInfo(
    @SerialName("id")
    val id: String? = null,
    @SerialName("headline")
    val headline: String? = null,
    @SerialName("author_name_cn")
    val author: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("piclink")
    val headshot: String? = null,
)

@Serializable
class Story (
    @Transient
    var teaser: Teaser? = null,

    val id: String = "",

    @SerialName("fileupdatetime")
    val createdAt: String,

    // Used as share's title
    @SerialName("cheadline")
    val titleCN: String,

    @SerialName("clongleadbody")
    val standfirstCN: String,

    @SerialName("cbody")
    val bodyCN: String,

    @SerialName("eheadline")
    val titleEN: String = "",

    @SerialName("elongleadbody")
    val standfirstEN: String = "",

    @SerialName("ebody")
    val bodyEN: String = "",

    @SerialName("ebyline_description")
    val orgEN: String = "",

    @SerialName("cbyline_description")
    val orgCN: String,

    @SerialName("cauthor")
    val authorCN: String? = "",

    @SerialName("cbyline_status")
    val locationCN: String,

    @SerialName("eauthor")
    val authorEN: String = "",

    @SerialName("ebyline_status")
    val locationEN: String = "",

    val tag: String,
    val genre: String,
    val topic: String,
    val industry: String,
    val area: String,

    // 0 - free
    // 1 - standard
    // 2 - premium
    @SerialName("accessright")
    val accessibleBy: String? = null,

    @SerialName("last_publish_time")
    val publishedAt: String,

    @SerialName("story_pic")
    val cover: StoryPic,

    @SerialName("story_audio")
    val aiAudios: AiAudio? = null,

    // Optional custom html snippet returned by interactive jsapi, often used
    // to embed a video player for original-voice videos.
    @SerialName("customHTML")
    val customHtml: String = "",

    @SerialName("columninfo")
    val columnInfo: ColumnInfo? = null,

    @SerialName("relative_story")
    val relatedStory: List<RelatedStory> = listOf(),
    val whitelist: Int = 0,
) {

    fun requireMemberTier(): Tier? {
        return when (accessibleBy) {
            "0" -> null
            "1" -> Tier.STANDARD
            "2" -> Tier.PREMIUM
            else -> null
        }
    }

    val permission: Permission
        get() = when {
            whitelist == 1 -> Permission.FREE
//            accessibleBy == "1" -> Permission.STANDARD
//            accessibleBy == "2" -> Permission.PREMIUM
            isSevenDaysOld() -> Permission.STANDARD
            else -> teaser?.permission() ?: Permission.FREE
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

    fun hasAudio(lang: Language): Boolean {
        if (aiAudios == null) {
            return false
        }

        return when (lang) {
            Language.CHINESE -> {
                aiAudios.chinese.isNotEmpty()
            }
            Language.ENGLISH, Language.BILINGUAL -> {
                aiAudios.english.isNotEmpty()
            }
        }
    }

    fun aiAudioTeaser(lang: Language): Teaser? {
        aiAudios ?: return null
        return Teaser(
            id = aiAudios.interactiveId,
            type = ArticleType.Interactive,
            subType = null,
            title = titleCN,
            langVariant = lang
        )
    }

    val isBilingual: Boolean
        get() = bodyEN.isNotBlank()

    val byline: String
        get() = "$orgCN $authorCN $locationCN"

    private val tags: List<String>
        get() = tag.split(",")

    /**
     * Example:
     * tag": "16周年好文精选,通货膨胀,货币政策,金融市场,全球经济",
     * area": "global",
     * genre": "column",
     * "topic": "economy",
     */
    val keywords: String
        get() = "$tag,$area,$topic,$genre"
                .replace(Regex(",+"), ",")
                .replace(Regex("^,"), "")
                .replace(Regex(",$"), "")

    private val fullText: String
        get() = "$bodyCN$keywords$titleCN$titleEN$bodyEN"

    fun getAdZone(homepageZone: String, fallbackZone: String, overrideAdZone: String): String {
        if (keywords.isBlank()) {
            return fallbackZone
        }

        for (sponsor in SponsorManager.sponsors) {
            if (sponsor.tag.isBlank()) {
                continue
            }

            if ( keywords.contains(sponsor.tag) ||
                keywords.contains(sponsor.title) ||
                sponsor.isKeywordsIn(fullText) ) {
                if (sponsor.zone.isNotEmpty()) {
                    return sponsor.normalizeZone()
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
            if (sponsor.isKeywordsIn(fullText) && sponsor.cntopic != "") {
                return sponsor.cntopic
            }
        }

        return ""
    }

    // Return shouldHideAd and sponsorTitle
    fun shouldHideAd(): Pair<Boolean, String?> {
        if (teaser == null) {
            return Pair(false, null)
        }

        if (teaser?.hideAd == true) {
            return Pair(true, null)
        }

        if (keywords.isBlank()) {
            return Pair(false, null)
        }

        if (keywords.contains(Keywords.removeAd)) {
            return Pair(true, null)
        }

        for (sponsor in SponsorManager.sponsors) {

            if (sponsor.tag.isBlank()) {
                continue
            }

            if (keywords.contains(sponsor.tag) || keywords.contains(sponsor.title)) {

                return Pair(
                    sponsor.hideAd == "yes",
                    if (sponsor.title.isNotBlank()) sponsor.title else null
                )
            }
        }

        return Pair(false, null)
    }

    fun pickAdchID(homepageId: String, fallbackId: String): String {

        if (keywords.isNotBlank()) {
            for (sponsor in SponsorManager.sponsors) {
                if ((keywords.contains(sponsor.tag) || keywords.contains(sponsor.title)) && sponsor.adid.isNotEmpty()) {
                    return sponsor.adid
                }
            }

            if (teaser?.channelMeta?.adid != homepageId) {
                return teaser?.channelMeta?.adid ?: ""
            }

            if (keywords.contains("lifestyle")) {
                return "1800"
            }

            if (keywords.contains("management")) {
                return "1700"
            }

            if (keywords.contains("opinion")) {
                return "1600"
            }

            if (keywords.contains("创新经济")) {
                return "2100"
            }

            if (keywords.contains("markets")) {
                return "1400"
            }

            if (keywords.contains("economy")) {
                return "1300"
            }

            if (keywords.contains("china")) {
                return "1100"
            }

            return "1200"
        }

        if (teaser?.channelMeta?.adid.isNullOrBlank()) {
            return fallbackId
        }
        return fallbackId
    }

    fun formatPublishTime(): String {
        return DateFormat.format("yyyy年M月d日 HH:mm", publishedAt.toLong() * 1000) as String
    }

    // HTML for the {related-stories}
    fun htmlForRelatedStories(): String {
        if (relatedStory.isEmpty()) {
            return ""
        }
        val listItems = relatedStory.mapIndexed { index, relatedStory ->
            """
            <li class="mp${index+1}"><a target="_blank" href="/story/${relatedStory.id}\">${relatedStory.titleCN ?: ""}</a></li>
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
        val reserved = arrayOf(
            "去广告",
            "单页",
            "透明",
            "置顶",
            "白底",
            "靠右",
            "沉底",
            "资料",
            "突发",
            "插图",
            "高清",
            "科技",
            "单选题",
            "置顶",
            "低调",
            "精华",
            "小测",
            "生活时尚",
            "测试",
            "视频",
            "新闻",
            "播音员朗读",
            "AI合成",
            "科技",
            "双语阅读",
            "高端专享",
            "订户专享",
            "会员专享",
            "双语电台",
            "高端限免",
            "限免",
            "去廣告",
            "單頁",
            "透明",
            "置頂",
            "白底",
            "靠右",
            "沈底",
            "資料",
            "突發",
            "插圖",
            "高清",
            "科技",
            "單選題",
            "置頂",
            "低調",
            "精華",
            "小測",
            "生活時尚",
            "測試",
            "視頻",
            "新聞",
            "播音員朗讀",
            "AI合成",
            "科技",
            "雙語閱讀",
            "高端專享",
            "訂戶專享",
            "會員專享",
            "雙語電臺",
            "高端限免",
            "限免",
            "QuizPlus",
            "SurveyPlus",
            "interactive_search",
            "FTLifeOfASong",
            "Podcast",
            "NoCopyrightCover",
            "AITranslation",
            "FTArticle",
            "IsEdited"
        )
        return tags
            .filter { s -> s !in reserved }
            .mapIndexed { index, s ->
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
        if (bodyCN.startsWith("<div class=\"pic")) {
            return ""
        }
        val imageUrl = cover.smallbutton
        if (imageUrl.isNullOrEmpty()) {
            return ""
        }
        return """
            <div class="story-image image" style="margin-bottom:0;">
                <figure data-url="${cover.smallbutton}" class="loading"></figure>
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

        return if (bodyCN.startsWith("<p>")) {
            arrBody.joinToString("")
        } else {
            arrBody.joinToString("") {
                if (it.startsWith("<") && it.endsWith(">")) {
                    // Assuming $it is already properly wrapped in a tag
                    it
                } else {
                    // Wrap $it in <p> tags
                    "<p>$it</p>"
                }
            }
        }
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

        return if (bodyEN.startsWith("<p>")) {
            arrBody.joinToString("")
        } else {
            arrBody.joinToString("") {
                "<p>$it</p>"
            }
        }
    }

    fun getBilingualBody(): String {
        if (bodyEN.isBlank()) {
            return ""
        }

        val alignedBody = alignBody()

        return if (bodyEN.startsWith("<p>")) {
            alignedBody.joinToString("") {
                "${it.en}${it.cn}"
            }
        } else {
            alignedBody.joinToString("") {
                "<p>${it.en}</p><p>${it.cn}</p>"
            }
        }
    }

    private fun splitCnBody(): List<String> {
        return bodyCN.split("\r\n").let {
            if (it.size > 1) {
                it
            } else {
                bodyCN.split("\n\n")
            }
        }
    }

    private fun splitEnBody(): List<String> {
        return bodyEN.split("\r\n").let {
            if (it.size > 1) {
                it
            } else {
                bodyEN.split("\n\n")
            }
        }
    }

    /**
     * Align english text to chinese text paragraph by paragraph.
     */
    private fun alignBody(): List<Bilingual> {
        val cnArray = splitCnBody()
        val enArray = splitEnBody()

        val bi = mutableListOf<Bilingual>()

        var cIndex = 0
        var eIndex = 0

        val cLen = cnArray.size
        val eLen = enArray.size


        while (cIndex < cLen && eIndex < eLen) {
            bi.add(Bilingual(
                cn = cnArray[cIndex],
                en = enArray[eIndex]),
            )
            cIndex++
            eIndex++
        }

        while (cIndex < cLen) {
            bi.add(Bilingual(
                cn = cnArray[cIndex],
                en = "",
            ))
            cIndex++
        }

        while (eIndex < eLen) {
            bi.add(Bilingual(
                cn = "",
                en = enArray[eIndex],
            ))
            eIndex++
        }

        return bi
    }
}
