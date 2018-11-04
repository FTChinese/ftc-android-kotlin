package com.ft.ftchinese.models

import android.content.Context
import android.content.res.Resources
import com.ft.ftchinese.R
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.Store
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*
import java.io.IOException

class Endpoints{
    companion object {
        const val HOST_FTC = "www.ftchinese.com"
        const val HOST_MAILBOX = "api003.ftmailbox.com"
        val hosts = arrayOf(HOST_FTC, HOST_MAILBOX)
    }
}

/**
 * The following data keys is used to parse JSON data passed from WebView by ContentWebViewInterface.postItems methods.
 * Most of them are useless, serving as placeholders so that we can extract the deep nested JSON values.
 * `ChannelMeta` represent the `meta` fieled in JSON data.
 */
data class ChannelContent(
        val meta: ChannelMeta,
        val sections: Array<ChannelSection>
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
        val lists: Array<ChannelList>
)

data class ChannelList(
        val name: String,
        val title: String,
        val preferLead: String,
        val sponsorAdId: String,
        val items: Array<ChannelItem>
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
 * `data-tier` for tier. Possible values: `story`, `interactive`,
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
        val type: String,
        val subType: String? = null,
        val headline: String,
        val shortlead: String? = null,
        val timeStamp: String? = null, // "1536249600"
        var keywords: String? = null,
        // These two properties are not parsed from JSON.
        // They are copy from ChannelMeata
        var adId: String = "",
        var adZone: String = "",
        var hideAd: Boolean = false
) : AnkoLogger {

    var standfirst: String = ""

    // Used for sharing
    val canonicalUrl: String
        get() = "http://www.ftchinese.com/$type/$id"

    val isSevenDaysOld: Boolean
        get() {
            if (timeStamp == null) {
                return false
            }

            val sevenDaysLater = Date((timeStamp.toLong() + 7 * 24 * 60 * 60) * 1000)
            val now = Date()

            if (sevenDaysLater.after(now)) {
                return false
            }

            return true
        }

    val isMembershipRequired: Boolean
        get() = isSevenDaysOld || type == TYPE_PREMIUM || subType == SUB_TYPE_RADIO

    private val filename: String
        get() = "${type}_$id.json"

    private val commentsId: String
        get() {
            return when(subType) {
                "interactive" -> "r_interactive_$id"
                "video" -> "r_video_$id"
                "story" -> id
                "photo", "photonews" -> "r_photo_$id"
                else -> "r_${type}_$id"
            }
        }

    private val commentsOrder: String
        get() {
            return "story"
        }

    // See Page/FTChinese/Main/APIs.swift
    // https://api003.ftmailbox.com/interactive/12339?bodyonly=no&webview=ftcapp&001&exclusive&hideheader=yes&ad=no&inNavigation=yes&for=audio&enableScript=yes&v=24
    val apiUrl: String?
        get() = when(type) {
            "story", "premium" -> "https://api.ftmailbox.com/index.php/jsapi/get_story_more_info/$id"

            "interactive" -> when (subType) {
                //"https://api003.ftmailbox.com/$type/$id?bodyonly=no&exclusive&hideheader=yes&ad=no&inNavigation=yes&for=audio&enableScript=yes&showAudioHTML=yes"
                SUB_TYPE_RADIO -> "https://api003.ftmailbox.com/$type/$id?bodyonly=yes&webview=ftcapp&i=3&001&exclusive"

                SUB_TYPE_MBAGYM -> canonicalUrl

                else -> "https://api003.ftmailbox.com/interactive/$id?bodyonly=no&webview=ftcapp&001&exclusive&hideheader=yes&ad=no&inNavigation=yes&for=audio&enableScript=yes&v=24"
            }

            "gym", "special" -> "https://api003.ftmailbox.com/$type/$id?bodyonly=yes&webview=ftcapp"

            "video" -> "https://api003.ftmailbox.com/$type/$id?bodyonly=yes&webview=ftcapp&004"

            else -> null
        }

    /**
     * @throws JsonSyntaxException
     */
    fun loadCachedStory(context: Context?): Story? {

        val jsonData = Store.load(context, filename) ?: return null

        return parseJson(jsonData)
    }

    /**
     * @throws JsonSyntaxException
     * @throws IOException
     * @throws IllegalStateException
     */
    fun fetchStory(context: Context): Story? {

        val url = apiUrl ?: return null

        info("Fetch a story from $url")
        val jsonData = Fetch().get(url).string() ?: return null

        GlobalScope.async {
            Store.save(context, filename, jsonData)
        }

        val story = parseJson(jsonData)

        keywords = story.keywords

        return story
    }

    /**
     * @throws JsonSyntaxException
     */
    private fun parseJson(jsonData: String): Story {

        val article = gson.fromJson<Story>(jsonData, Story::class.java)
        standfirst = article.clongleadbody

        return article
    }

    private fun pickAdZone(homepageZone: String, fallbackZone: String): String {
        if (!keywords.isNullOrBlank()) {
            return fallbackZone
        }

        for (sponsor in SponsorManager.sponsors) {
            if ((keywords?.contains(sponsor.tag) == true || keywords?.contains(sponsor.title) == true) && sponsor.zone.isNotEmpty() ) {
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

        if (keywords?.contains("lifestyle") == true) {
            return "lifestyle"
        }

        if (keywords?.contains("management") == true) {
            return "management"
        }

        if (keywords?.contains("opinion") == true) {
            return "opinion"
        }

        if (keywords?.contains("创新经济") == true) {
            return "创新经济"
        }

        if (keywords?.contains("markets") == true) {
            return "markets"
        }

        if (keywords?.contains("economy") == true) {
            return "economy"
        }

        if (keywords?.contains("china") == true) {
            return "china"
        }

        return fallbackZone
    }

    private fun pickAdchID(homepageId: String, fallbackId: String): String {
        if (!keywords.isNullOrBlank()) {
            for (sponsor in SponsorManager.sponsors) {
                if ((keywords?.contains(sponsor.tag) == true || keywords?.contains(sponsor.title) == true) && sponsor.adid.isNotEmpty()) {
                    return sponsor.adid
                }
            }

            if (adId != homepageId) {
                return adId
            }

            if (keywords?.contains("lifestyle") == true) {
                return "1800"
            }

            if (keywords?.contains("management") == true) {
                return "1700"
            }

            if (keywords?.contains("opinion") == true) {
                return "1600"
            }

            if (keywords?.contains("创新经济") == true) {
                return "2100"
            }

            if (keywords?.contains("markets") == true) {
                return "1400"
            }

            if (keywords?.contains("economy") == true) {
                return "1300"
            }

            if (keywords?.contains("china") == true) {
                return "1100"
            }

            return "1200"
        }

        if (adId.isNotEmpty()) {
            return fallbackId
        }
        return fallbackId
    }

    fun render(template: String?, article: Story?, language: Int, follows: JSFollows): String? {

        if (template == null || article == null) {
            return null
        }

        var shouldHideAd = false

        if (hideAd) {
            shouldHideAd = true
        } else if (!keywords.isNullOrBlank()) {
            if (keywords?.contains(Keywords.removeAd) == true) {
                shouldHideAd = true
            } else {
                for (sponsor in SponsorManager.sponsors) {
                    if (keywords?.contains(sponsor.tag) == true || keywords?.contains(sponsor.title) == true) {
                        shouldHideAd = sponsor.hideAd == "yes"
                        break
                    }
                }
            }
        }


        val adMPU = if (shouldHideAd) "" else AdParser.getAdCode(AdPosition.MIDDLE_ONE)

        var body = ""
        var title = ""

        when (language) {
            LANGUAGE_CN -> {
                body = article.getCnBody(withAd = !shouldHideAd)
                title = article.title.cn
            }
            LANGUAGE_EN -> {
                body = article.getEnBody(withAd = !shouldHideAd)
                title = article.title.en ?: ""
            }
            LANGUAGE_BI -> {
                body = article.bodyAlignedXML
                title = "${article.title.cn}<br>${article.title.en}"
            }
        }

        val storyHTMLOriginal = template
                .replace("{story-tag}", article.tag)
                .replace("{story-author}", article.cauthor)
                .replace("{story-genre}", article.genre)
                .replace("{story-area}", article.area)
                .replace("{story-industry}", article.industry)
                .replace("{story-main-topic}", "")
                .replace("{story-sub-topic}", "")
                .replace("{adchID}", pickAdchID(HOME_AD_CH_ID, DEFAULT_STORY_AD_CH_ID))
                .replace("{comments-id}", commentsId)
                .replace("{story-theme}", article.htmlForTheme())
                .replace("{story-headline}", title)
                .replace("{story-lead}", article.standfirst)
                .replace("{story-image}", article.htmlForCoverImage())
                .replace("{story-time}", article.createdAt)
                .replace("{story-byline}", article.byline)
                .replace("{story-body}", body)
                .replace("{story-id}", article.id)
                .replace("{related-stories}", article.htmlForRelatedStories())
                .replace("{related-topics}", article.htmlForRelatedTopics())
                .replace("{comments-order}", commentsOrder)
                .replace("{story-container-style}", "")
                .replace("'{follow-tags}'", follows.tag)
                .replace("'{follow-topic}'", follows.topic)
                .replace("'{follow-industry}'", follows.industry)
                .replace("'{follow-area}'", follows.area)
                .replace("'{follow-augthor}'", follows.author)
                .replace("'{follow-column}'", follows.column)
                .replace("{ad-zone}", pickAdZone(HOME_AD_ZONE, DEFAULT_STORY_AD_ZONE))
                .replace("{ad-mpu}", adMPU)
                //                        .replace("{font-class}", "")
                .replace("{{googletagservices-js}}", JSCodes.googletagservices)

        val storyHTML = AdParser.updateAdCode(storyHTMLOriginal, shouldHideAd)

        val storyHTMLCheckingVideo = JSCodes.getInlineVideo(storyHTML)

        return JSCodes.getCleanHTML(storyHTMLCheckingVideo)

    }


    companion object {
        private const val TAG = "ChannelItem"
        private const val PREF_NAME_FAVOURITE = "favourite"
        const val LANGUAGE_CN = 0
        const val LANGUAGE_EN = 1
        const val LANGUAGE_BI = 2
        const val TYPE_STORY = "story"
        const val TYPE_PREMIUM = "premium"
        const val TYPE_INTERACTIVE = "interactive"
        const val SUB_TYPE_RADIO = "radio"
        const val SUB_TYPE_USER_COMMENT = ""
        const val SUB_TYPE_MBAGYM = "mbagym"

        const val HOME_AD_ZONE = "home"
        const val DEFAULT_STORY_AD_ZONE = "world"

        const val HOME_AD_CH_ID = "1000"
        const val DEFAULT_STORY_AD_CH_ID = "1200"
    }
}

val pathToTitle = mapOf(
        "businesscase.html" to "中国商业案例精选",
        "editorchoice-issue.html" to "编辑精选",
        "chinabusinesswatch.html" to "宝珀·中国商业观察",
        "viewtop.html" to "高端视点",
        "Emotech2017.html" to "2018·预见人工智能",
        "antfinancial.html" to "“新四大发明”背后的中国浪潮",
        "teawithft.html" to "与FT共进下午茶",
        "creditease.html" to "未来生活 未来金融",
        "markets.html" to "金融市场",
        "hxxf2016.html" to "透视中国PPP模式",
        "money.html" to "理财"
)