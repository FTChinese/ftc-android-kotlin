package com.ft.ftchinese.model.content

import android.text.format.DateFormat
import com.beust.klaxon.Json
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.tracking.*
import org.jetbrains.anko.AnkoLogger
import java.util.*

data class Bilingual(
    var cn: String,
    var en: String?
)

data class StoryPic(
    val smallbutton: String,
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

data class AiAudio(
    @Json(name = "ai_audio_e")
    val english: String = "",
    @Json(name = "ai_audio_c")
    val chinese: String = "",
    @Json(name = "interactive_id")
    val interactiveId: String = "",
)

class Story (
    @Json(ignored = true)
    var teaser: Teaser? = null,

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
    val authorCN: String? = "",

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
    val accessibleBy: String? = null,

    @Json(name = "last_publish_time")
    val publishedAt: String,

    @Json(name = "story_pic")
    val cover: StoryPic,

    @Json(name = "story_audio")
    val aiAudios: AiAudio? = null,

    @Json(name = "relative_story")
    val relatedStory: List<RelatedStory> = listOf(),
    val whitelist: Int = 0,
) : AnkoLogger {

    fun isFrom(t: Teaser): Boolean {
        return id == t.id &&  teaser?.type == t.type
    }

    fun requireMemberTier(): Tier? {
        return when (accessibleBy) {
            "0" -> null
            "1" -> Tier.STANDARD
            "2" -> Tier.PREMIUM
            else -> null
        }
    }

    fun permission(): Permission {
        return when {
            whitelist == 1 -> Permission.FREE
//            accessibleBy == "1" -> Permission.STANDARD
//            accessibleBy == "2" -> Permission.PREMIUM
            isSevenDaysOld() -> Permission.STANDARD
            else -> teaser?.permission() ?: Permission.FREE
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

    fun audioUrl(lang: Language): String? {
        aiAudios ?: return null

        return when (lang) {
            Language.CHINESE -> aiAudios.chinese
            Language.ENGLISH, Language.BILINGUAL -> aiAudios.english
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
                "<p>$it</p>"
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
