package com.ft.ftchinese.model.content

import android.net.Uri
import android.os.Parcelable
import com.beust.klaxon.Json
import com.ft.ftchinese.model.fetch.KArticleType
import com.ft.ftchinese.model.reader.Permission
import kotlinx.parcelize.Parcelize
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*

/**
 * The following data keys is used to parse JSON data passed from WebView by ContentWebViewInterface.postItems methods.
 * Most of them are useless, serving as placeholders so that we can extract the deep nested JSON values.
 * `ChannelMeta` represent the `meta` fieled in JSON data.
 */
data class ChannelContent(
    val meta: ChannelMeta,
    val sections: List<ChannelSection>
)

@Parcelize
data class ChannelMeta(
    val title: String,
    val description: String,
    val theme: String,
    val adid: String,
    val adZone: String = "home"
) : Parcelable


data class ChannelSection(
    val type: String,
    val title: String,
    val name: String,
    val side: String,
    val sideAlign: String,
    val lists: List<ChannelList>
)

data class ChannelList(
    val name: String,
    val title: String,
    val preferLead: String,
    val sponsorAdId: String,
    val items: List<Teaser>
)
/**
 * ChannelItem represents an item in a page of ViewPager.
 * This is the data tier passed to AbsContentActivity so that it know what kind of data to load.
 * iOS equivalent might be Page/Layouts/Content/ContentItem.swift#ContentItem
 * The fields are collected from all HTML elements `div.item-container-app`.
 * See https://github.com/FTChinese/android-client/app/scripts/list.js.
 *
 * In short it used those attributes:
 * `data-id` for `id`
 * `data-type` for tier. Possible values: `story`, `interactive`,
 * The content of `a.item-headline-link` inside `div.item-container-app` for `headline`
 * `data-audio` for `shortlead`
 * `data-caudio` for `caudio`
 * `data-eaudio` for `eaudio`
 * `data-sub-tier` for `subType`. Possible values: `radio`, `speedreading`
 * `data-date` for `timeStamp`
 *
 * The fields in ChannelItem are also persisted to SQLite when user clicked on it.
 * It seems the Room library does not work well with Kotlin data class. Use a plain class works.
 *
 * This class is also used to record reading history. `standfirst` is used only for this purpose. `subType` and `shortlead` should not be used for this purpose. ArticleStore could only recored `type==story`.
 */
@Parcelize
data class Teaser(
    val id: String, // The only field required to build jsapi url in case type is Story or Premium.
    // For column type, you should start a ChannelActivity instead of  StoryActivity.
    @KArticleType
    val type: ArticleType, // story | premium | video | photonews | interactive | column
    val subType: String? = null, // speedreading | radio | bilingual to show cn-en content.

    @Json(name = "headline")
    var title: String,

    @Json(name = "eaudio")
    val audioUrl: String? = null,

    @Json(name = "shortlead")
    val radioUrl: String? = null, // this is a webUrl of mp3 for subType radio.

    @Json(name = "timeStamp")
    val publishedAt: String? = null, // "1536249600"

    // "线下活动,企业公告,会员专享"
    val tag: String = "",

    // Whether this instance is created by analysing url. In such case, teaser contains only id and type.
    // For type == story | premium, you should update it based on the loaded story;
    // for other types, you could only update the teaser from open graph.
    @Json(ignored = true)
    val isCreatedFromUrl: Boolean = false,

    // These properties are not parsed from JSON.
    // Copied from ChannelMeta
    @Json(ignored = true)
    var hideAd: Boolean = false,
    @Json(ignored = true)
    var langVariant: Language? = null,
    @Json(ignored = true)
    var channelPerm: Permission? = null,
    @Json(ignored = true)
    var channelMeta: ChannelMeta? = null,
) : Parcelable, AnkoLogger {

    // Only stories have api.
    fun hasJsAPI(): Boolean {
        return type == ArticleType.Story || type == ArticleType.Premium || (type == ArticleType.Interactive && subType == "bilingual")
    }

    fun apiPathSegment(): String {
        return when (type) {
            ArticleType.Story, ArticleType.Premium -> "/index.php/jsapi/get_story_more_info/${id}"
            ArticleType.Interactive -> "/index.php/jsapi/get_interactive_more_info/${id}"
            else -> ""
        }
    }

    fun hasMp3(): Boolean {
        return !audioUrl.isNullOrBlank() || !radioUrl.isNullOrBlank()
    }

    fun audioUri(): Uri? {
        val url =  audioUrl ?: radioUrl ?: return null
        return try {
            Uri.parse(url)
        } catch (e: Exception) {
            null
        }
    }

    fun buildGALabel(): String {
        return when (type) {
            ArticleType.Story -> when {
                langVariant == Language.ENGLISH -> "EnglishText/story/$id/en"
                langVariant == Language.BILINGUAL -> "EnglishText/story/$id/ce"
                isSevenDaysOld() -> "Archive/story/$id"
                else -> "ExclusiveContent/premium/$id"
            }
            ArticleType.Premium -> {
                "ExclusiveContent/premium/$id"
            }
            ArticleType.Interactive -> when (subType) {
                SUB_TYPE_RADIO -> "Radio/interactive/$id"
                SUB_TYPE_SPEED_READING -> "SpeedReading/interactive/$id"
                else -> when {
                    tag.contains("FT研究院") && tag.contains("会员专享") -> "StandardIntelligence/interactive/$id"
                    tag.contains("FT研究院") && tag.contains("高端专享") -> "PremiumIntelligence/interactive/$id"
                    tag.contains("会员专享") -> "Standard/interactive/$id"
                    tag.contains("高端专享") -> "Premium/interactive/$id"
                    else -> "$type/$id"
                }
            }
            else -> "$type/$id"
        }
    }

    fun withMeta(meta: ChannelMeta?): Teaser {
        channelMeta = meta

        return this
    }

    // Pass permission from hosting channel.
    fun withParentPerm(p: Permission?): Teaser {
        channelPerm = p
        return this
    }

    private fun isSevenDaysOld(): Boolean {
        if (publishedAt == null) {
            return false
        }

        val sevenDaysLater = Date((publishedAt.toLong() + 7 * 24 * 60 * 60) * 1000)
        val now = Date()

        if (sevenDaysLater.after(now)) {
            return false
        }

        return true
    }

    // File name used to cache/retrieve json data.
    fun cacheNameJson(): String {
        return "${type}_$id.json"
    }

    val cacheNameHtml: String
        get() = "${type}_$id.html"

    fun screenshotName(): String {
        return "${type}_$id"
    }
    
    fun permission(): Permission {
        val p = channelPerm
        if (p != null) {
            return p
        }

        if (tag.contains("会员专享")) {
            return Permission.STANDARD
        }

        if (tag.contains("高端专享")) {
            return Permission.PREMIUM
        }

        if (type == ArticleType.Premium) {
            return Permission.STANDARD
        }

        if (type == ArticleType.Interactive) {
            info("An interactive")
            return when (subType) {
                SUB_TYPE_RADIO,
                SUB_TYPE_SPEED_READING -> Permission.STANDARD
                else -> if (isSevenDaysOld()) {
                    Permission.STANDARD
                } else {
                    Permission.FREE
                }
            }
        }

        if (isSevenDaysOld()) {
            return Permission.STANDARD
        }

        return Permission.FREE
    }

    fun getCommentsId(): String {
        return when(subType) {
            "interactive" -> "r_interactive_$id"
            "video" -> "r_video_$id"
            "story" -> id
            "photo", "photonews" -> "r_photo_$id"
            else -> "r_${type}_$id"
        }
    }

    fun getCommentsOrder(): String {
        return "story"
    }

    // Used for standard restful api.
    fun apiCacheFileName(lang: Language): String {
        return "${type}_${id}_${lang}.api.json"
    }

    companion object {

        const val SUB_TYPE_RADIO = "radio"
        const val SUB_TYPE_MBAGYM = "mbagym"
        const val SUB_TYPE_SPEED_READING = "speedreading"

        const val HOME_AD_ZONE = "home"
        const val DEFAULT_STORY_AD_ZONE = "world"

        const val HOME_AD_CH_ID = "1000"
        const val DEFAULT_STORY_AD_CH_ID = "1200"
    }
}


