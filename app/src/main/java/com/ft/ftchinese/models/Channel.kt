package com.ft.ftchinese.models

import com.beust.klaxon.Json
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.util.FTC_OFFICIAL_URL
import com.ft.ftchinese.util.MAILBOX_URL
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

data class ChannelMeta(
        val title: String,
        val description: String,
        val theme: String,
        val adid: String,
        val adZone: String = "home"
)


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
        val items: List<ChannelItem>
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
data class ChannelItem(
        val id: String,
        // For column type, you should start a ChannelActivity instead of  StoryActivity.
        val type: String, // story | premium | video | interactive | column |
        val subType: String? = null, // speedreading | radio

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

        @Json(ignored = true)
        var webUrl: String = "",

        @Json(ignored = true)
        var isWebpage: Boolean = false
) : AnkoLogger {

    // These two properties are not parsed from JSON.
    // They are copy from ChannelMeta
    var channelTitle: String = ""
    var theme: String = "default"
    var adId: String = ""
    var adZone: String = ""
    var hideAd: Boolean = false

    fun withMeta(meta: ChannelMeta?): ChannelItem {
        if (meta == null) {
            return this;
        }
        channelTitle = meta.title
        theme = meta.theme
        adId = meta.adid
        adZone = meta.adZone
        
        return this
    }
    
    fun toStarredArticle(): StarredArticle {
        return StarredArticle(
                id = id,
                type = type,
                subType = subType ?: "",
                title = title,
                standfirst = "",
                keywords = "",
                imageUrl = "",
                audioUrl = audioUrl ?: "",
                radioUrl = radioUrl ?: "",
                publishedAt = publishedAt ?: "",
                webUrl = getCanonicalUrl()
        )
    }

    // Used for sharing
    fun getCanonicalUrl(): String {
        if (webUrl.isNotBlank()) {
            return webUrl
        }

        return "$FTC_OFFICIAL_URL/$type/$id"
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

    fun cacheNameHtml(): String {
        return "${type}_$id.html"
    }

    fun requireStandard(): Boolean {
        if (isSevenDaysOld()) {
            return true
        }

        if (tag.contains("会员专享")) {
            return true
        }

        if (type == TYPE_INTERACTIVE) {
            info("An interactive")
            return when (subType) {
                SUB_TYPE_RADIO,
                SUB_TYPE_SPEED_READING -> true
                else -> false
            }
        }

        return false
    }

    fun requirePremium(): Boolean {
        if (isSevenDaysOld()) {
            return true
        }

        if (type == "premium") {
            return true
        }

        if (tag.contains("会员专享")) {
            return true
        }

        if (type == TYPE_INTERACTIVE) {
            return when (subType) {
                SUB_TYPE_RADIO,
                SUB_TYPE_SPEED_READING -> true
                else -> false
            }
        }

        return false
    }

    private fun getCommentsId(): String {
        return when(subType) {
            "interactive" -> "r_interactive_$id"
            "video" -> "r_video_$id"
            "story" -> id
            "photo", "photonews" -> "r_photo_$id"
            else -> "r_${type}_$id"
        }
    }

    private fun getCommentsOrder(): String {
        return "story"
    }

    /**
     * URL used to fetch an article
     * See Page/FTChinese/Main/APIs.swift
     * https://api003.ftmailbox.com/interactive/12339?bodyonly=no&webview=ftcapp&001&exclusive&hideheader=yes&ad=no&inNavigation=yes&for=audio&enableScript=yes&v=24
     */
    fun buildApiUrl(): String {
        if (id.isBlank() || type.isBlank()) {
            return ""
        }

        return when(type) {
            TYPE_STORY, TYPE_PREMIUM -> "$MAILBOX_URL/index.php/jsapi/get_story_more_info/$id"

            TYPE_COLUMN -> "$MAILBOX_URL/$type/$id?bodyonly=yes&webview=ftcapp&bodyonly=yes"

            TYPE_INTERACTIVE -> when (subType) {
                //"https://api003.ftmailbox.com/$type/$id?bodyonly=no&exclusive&hideheader=yes&ad=no&inNavigation=yes&for=audio&enableScript=yes&showAudioHTML=yes"
                SUB_TYPE_RADIO -> "$MAILBOX_URL/$type/$id?bodyonly=yes&webview=ftcapp&i=3&001&exclusive"
                SUB_TYPE_SPEED_READING -> "$FTC_OFFICIAL_URL/$type/$id?bodyonly=yes&webview=ftcapp&i=3&001&exclusive"

                SUB_TYPE_MBAGYM -> "$FTC_OFFICIAL_URL/$type/$id"

                else -> "$MAILBOX_URL/$type/$id?bodyonly=no&webview=ftcapp&001&exclusive&hideheader=yes&ad=no&inNavigation=yes&for=audio&enableScript=yes&v=24"
            }

            TYPE_VIDEO -> "$FTC_OFFICIAL_URL/$type/$id?bodyonly=yes&webview=ftcapp&004"

            else -> "$MAILBOX_URL/$type/$id?webview=ftcapp"
        }
    }

    private fun pickAdZone(homepageZone: String, fallbackZone: String, keywords: String): String {
        if (!keywords.isBlank()) {
            return fallbackZone
        }

        for (sponsor in SponsorManager.sponsors) {
            if ((keywords.contains(sponsor.tag) || keywords.contains(sponsor.title)) && sponsor.zone.isNotEmpty() ) {
                return if (sponsor.zone.contains("/")) {
                    sponsor.zone
                } else {
                    "home/special/${sponsor.zone}"
                }
            }
        }

        if (adZone != homepageZone) {
            return adZone
        }

        if (keywords.contains("lifestyle")) {
            return "lifestyle"
        }

        if (keywords.contains("management")) {
            return "management"
        }

        if (keywords.contains("opinion")) {
            return "opinion"
        }

        if (keywords.contains("创新经济")) {
            return "创新经济"
        }

        if (keywords.contains("markets")) {
            return "markets"
        }

        if (keywords.contains("economy")) {
            return "economy"
        }

        if (keywords.contains("china")) {
            return "china"
        }

        return fallbackZone
    }

    private fun pickAdchID(homepageId: String, fallbackId: String, keywords: String): String {
        if (!keywords.isBlank()) {
            for (sponsor in SponsorManager.sponsors) {
                if ((keywords.contains(sponsor.tag) || keywords.contains(sponsor.title)) && sponsor.adid.isNotEmpty()) {
                    return sponsor.adid
                }
            }

            if (adId != homepageId) {
                return adId
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

        if (adId.isNotEmpty()) {
            return fallbackId
        }
        return fallbackId
    }

    fun renderStory(template: String?, story: Story?, language: Language, follows: JSFollows): String? {

        if (template == null || story == null) {

            return null
        }

        var shouldHideAd = false
        var sponsorTitle: String? = null

        if (hideAd) {
            shouldHideAd = true
        } else if (!story.keywords.isBlank()) {
            info("Story keywords: ${story.keywords}")

            if (story.keywords.contains(Keywords.removeAd)) {
                shouldHideAd = true
            } else {
                for (sponsor in SponsorManager.sponsors) {
                    if (story.keywords.contains(sponsor.tag) || story.keywords.contains(sponsor.title)) {
                        shouldHideAd = (sponsor.hideAd == "yes")
                        sponsorTitle = sponsor.title
                        break
                    }
                }
            }
        }


        val adMPU = if (shouldHideAd) "" else AdParser.getAdCode(AdPosition.MIDDLE_ONE)

        var body = ""
        var title = ""
        var lang = ""

        when (language) {
            Language.CHINESE -> {
                body = story.getCnBody(withAd = !shouldHideAd)
                title = story.titleCN
            }
            Language.ENGLISH -> {
                body = story.getEnBody(withAd = !shouldHideAd)
                title = story.titleEN
            }
            Language.BILINGUAL -> {
                body = story.getBilingualBody()
                title = "${story.titleCN}<br>${story.titleEN}"
                lang = "ce"
            }
        }

        // {story-language-class}
        val storyHTMLOriginal = template
                .replace("{story-tag}", story.tag)
                .replace("{story-author}", story.authorCN)
                .replace("{story-genre}", story.genre)
                .replace("{story-area}", story.area)
                .replace("{story-industry}", story.industry)
                .replace("{story-main-topic}", "")
                .replace("{story-sub-topic}", "")
                .replace("{adchID}", pickAdchID(HOME_AD_CH_ID, DEFAULT_STORY_AD_CH_ID, story.keywords))
                .replace("{comments-id}", getCommentsId())
                .replace("{story-theme}", story.htmlForTheme(sponsorTitle))
                .replace("{story-headline}", title)
                .replace("{story-lead}", story.standfirstCN)
                .replace("{story-image}", story.htmlForCoverImage())
                .replace("{story-time}", story.formatPublishTime())
                .replace("{story-byline}", story.byline)
                .replace("{story-body}", body)
                .replace("{story-id}", story.id)
                .replace("{related-stories}", story.htmlForRelatedStories())
                .replace("{related-topics}", story.htmlForRelatedTopics())
                .replace("{comments-order}", getCommentsOrder())
                .replace("{story-container-style}", "")
                .replace("'{follow-tags}'", follows.tag)
                .replace("'{follow-topic}'", follows.topic)
                .replace("'{follow-industry}'", follows.industry)
                .replace("'{follow-area}'", follows.area)
                .replace("'{follow-augthor}'", follows.author)
                .replace("'{follow-column}'", follows.column)
                .replace("{ad-zone}", pickAdZone(HOME_AD_ZONE, DEFAULT_STORY_AD_ZONE, story.keywords))
                .replace("{ad-mpu}", adMPU)
                //                        .replace("{font-class}", "")
                .replace("{{googletagservices-js}}", JSCodes.googletagservices)
                .replace("{story-language-class}", lang)

        val storyHTML = AdParser.updateAdCode(storyHTMLOriginal, shouldHideAd)

        val storyHTMLCheckingVideo = JSCodes.getInlineVideo(storyHTML)

        return JSCodes.getCleanHTML(storyHTMLCheckingVideo)

    }

    companion object {
        const val TYPE_STORY = "story"
        const val TYPE_PREMIUM = "premium"
        const val TYPE_VIDEO = "video"
        const val TYPE_INTERACTIVE = "interactive"
        const val TYPE_COLUMN = "column"
        const val TYPE_PHOTO_NEWS = "photonews"
        const val TYPE_CHANNEL = "channel"
        const val TYPE_TAG = "tag"
        const val TYPE_M = "m"

        const val SUB_TYPE_RADIO = "radio"
        const val SUB_TYPE_USER_COMMENT = ""
        const val SUB_TYPE_MBAGYM = "mbagym"
        const val SUB_TYPE_SPEED_READING = "speedreading"

        const val SUB_TYPE_CORP = "corp"
        const val SUB_TYPE_MARKETING = "marketing"

        const val HOME_AD_ZONE = "home"
        const val DEFAULT_STORY_AD_ZONE = "world"

        const val HOME_AD_CH_ID = "1000"
        const val DEFAULT_STORY_AD_CH_ID = "1200"
    }
}

enum class Language {
    ENGLISH,
    CHINESE,
    BILINGUAL
}
