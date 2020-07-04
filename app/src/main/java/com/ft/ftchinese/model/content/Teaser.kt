package com.ft.ftchinese.model.content

import android.net.Uri
import android.os.Parcelable
import com.beust.klaxon.Json
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.repository.HOST_FTC
import com.ft.ftchinese.util.KArticleType
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
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
        val id: String,
        // For column type, you should start a ChannelActivity instead of  StoryActivity.
        @KArticleType
        val type: ArticleType, // story | premium | video | photonews | interactive | column
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

        // What's the purpose of this one?
        @Json(ignored = true)
        var webUrl: String = "",

        @Json(ignored = true)
        var isWebpage: Boolean = false
) : Parcelable, AnkoLogger {

    // These two properties are not parsed from JSON.
    // They are copy from ChannelMeta
    @IgnoredOnParcel
    var channelTitle: String = ""
    @IgnoredOnParcel
    var theme: String = "default"
    @IgnoredOnParcel
    var adId: String = ""
    @IgnoredOnParcel
    var adZone: String = ""
    @IgnoredOnParcel
    var hideAd: Boolean = false

    @IgnoredOnParcel
    var langVariant: Language? = null

    fun hasRestfulAPI(): Boolean {
        return type == ArticleType.Story || type == ArticleType.Premium
    }

    fun hasMp3(): Boolean {
        return audioUrl != null || radioUrl != null
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
        if (meta == null) {
            return this
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
                type = type.toString(),
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

        return "https://$HOST_FTC/$type/$id"
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


    fun permission(): Permission {

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

    fun apiCacheFileName(lang: Language): String {
        return "${type}_${id}_${lang}.api.json"
    }

    companion object {

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

fun buildTeaserFromUri(uri: Uri): Teaser {
    return Teaser(
        id = uri.lastPathSegment ?: "",
        type = ArticleType.fromString(uri.pathSegments[0]),
        title = "",
        webUrl = uri.toString()
    )
}

// TabPages supply the data for TagPagerAdapter under a BottomNavigationView item.
object TabPages {

    val newsPages = listOf(

        ChannelSource(
            title = "首页",
            name = "news_home",
            contentPath = "/?webview=ftcapp&bodyonly=yes&maxB=1&backupfile=localbackup&showIAP=yes&pagetype=home&001",
            htmlType = HTML_TYPE_FRAGMENT
        ),

        ChannelSource(
            title = "中国",
            name = "news_china",
            contentPath = "/channel/china.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),

        ChannelSource(
            title = "标准订阅",
            name = "news_standard_only",
            contentPath = "/channel/standardsubscription.html?webview=ftcapp&bodyonly=yes&ad=no&001",
            htmlType = HTML_TYPE_FRAGMENT,
            permission = Permission.STANDARD),

        ChannelSource(
            title = "高端订阅",
            name = "news_premium_only",
            contentPath = "/channel/premiumsubscription.html?webview=ftcapp&bodyonly=yes&ad=no&showEnglishAudio=yes&018",
            htmlType = HTML_TYPE_FRAGMENT,
            permission = Permission.PREMIUM),

        /**
             * "meta": {
                "title": "全球",
                "description": "",
                "theme": "default",
                "adid": "1200",
                "adZone": "world"
                }
             *
             */

            /**
             * "meta": {
                "title": "全球",
                "description": "",
                "theme": "default",
                "adid": "1200",
                "adZone": "world"
                }
             *
             */
        ChannelSource(
            title = "全球",
            name = "news_global",
            contentPath = "/channel/world.html?webview=ftcapp&bodyonly=yes&002",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * "meta": {
                "title": "观点",
                "description": "",
                "theme": "default",
                "adid": "1600",
                "adZone": "opinion"
                }
             */
            /**
             * "meta": {
                "title": "观点",
                "description": "",
                "theme": "default",
                "adid": "1600",
                "adZone": "opinion"
                }
             */
        ChannelSource(
            title = "观点",
            name = "news_opinions",
            contentPath = "/channel/opinion.html?webview=ftcapp&bodyonly=yes&ad=no",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * "meta": {
             *   "title": "专栏",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1600",
             *   "adZone": "opinion/column"
             *   }
             *
             * {
             *  "id": "007000049",
             *  "type": "column",
             *  "headline": "徐瑾经济人"
             *  }
             * You have to compare type == column. If true,
             * start a ChannelActivity.
             *
             * After you opened a column, it's another list of articles.
             * The data looks like:
             * "meta": {
             *   "title": "《徐瑾经济人》",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1300",
             *   "adZone": "economy/economics"
             *   }
             */
            /**
             * "meta": {
             *   "title": "专栏",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1600",
             *   "adZone": "opinion/column"
             *   }
             *
             * {
             *  "id": "007000049",
             *  "type": "column",
             *  "headline": "徐瑾经济人"
             *  }
             * You have to compare type == column. If true,
             * start a ChannelActivity.
             *
             * After you opened a column, it's another list of articles.
             * The data looks like:
             * "meta": {
             *   "title": "《徐瑾经济人》",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1300",
             *   "adZone": "economy/economics"
             *   }
             */
        ChannelSource(
            title = "专栏",
            name = "news_column",
            contentPath = "/channel/column.html?webview=ftcapp&bodyonly=yes&ad=no",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * "meta": {
             *   "title": "金融市场",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1400",
             *   "adZone": "markets"
             *   }
             */
            /**
             * "meta": {
             *   "title": "金融市场",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1400",
             *   "adZone": "markets"
             *   }
             */
        ChannelSource(
            title = "金融市场",
            name = "news_markets",
            contentPath = "/channel/markets.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * "meta": {
             *   "title": "金融市场",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1400",
             *   "adZone": "markets"
             *   }
             */
            /**
             * "meta": {
             *   "title": "金融市场",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1400",
             *   "adZone": "markets"
             *   }
             */
        ChannelSource(
            title = "商业",
            name = "news_business",
            contentPath = "/channel/business.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
        ChannelSource(
            title = "经济",
            name = "news_economy",
            contentPath = "/channel/economy.html?webview=ftcapp&bodyonly=yes&001",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * "meta": {
             *   "title": "科技",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1502",
             *   "adZone": "business/technology"
             *   }
             */
            /**
             * "meta": {
             *   "title": "科技",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1502",
             *   "adZone": "business/technology"
             *   }
             */
        ChannelSource(
            title = "科技",
            name = "news_technology",
            contentPath = "/channel/technology.html?webview=ftcapp&bodyonly=yes&001",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * "meta": {
             *   "title": "教育",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "2200",
             *   "adZone": "education"
             *   }
             */
            /**
             * "meta": {
             *   "title": "教育",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "2200",
             *   "adZone": "education"
             *   }
             */
        ChannelSource(
            title = "教育",
            name = "news_education",
            contentPath = "/channel/education.html?webview=ftcapp&bodyonly=yes&001",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * "meta": {
             *   "title": "管理",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1700",
             *   "adZone": "management"
             *   }
             */
            /**
             * "meta": {
             *   "title": "管理",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1700",
             *   "adZone": "management"
             *   }
             */
        ChannelSource(
            title = "管理",
            name = "news_management",
            contentPath = "/channel/management.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * "meta": {
             *   "title": "生活时尚",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1800",
             *   "adZone": "lifestyle"
             *   }
             */
            /**
             * "meta": {
             *   "title": "生活时尚",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1800",
             *   "adZone": "lifestyle"
             *   }
             */
        ChannelSource(
            title = "生活时尚",
            name = "news_life_style",
            contentPath = "/channel/lifestyle.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * Load it directly into WebView.
             * Handle URL in ChannelWebViewClient.
             * Most links should start a ChannelActivity
             *
             * URL patterns on this page:
             * http://www.ftchinese.com/tag/璀璨星耀
             * http://www.ftchinese.com/channel/chinabusinesswatch.html
             * http://www.ftchinese.com/channel/tradewar.html
             * http://www.ftchinese.com/m/corp/preview.html?pageid=huawei2018
             * http://www.ftchinese.com/m/marketing/Emotech2017.html
             * http://www.ftchinese.com/m/corp/preview.html?pageid=alibaba2018
             *
             * After clicked a link, it starts a ChannelActivity:
             * "meta": {
             *   "title": "璀璨星耀",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "5071",
             *   "adZone": "home/special/brightstar"
             *   }
             */
            /**
             * Load it directly into WebView.
             * Handle URL in ChannelWebViewClient.
             * Most links should start a ChannelActivity
             *
             * URL patterns on this page:
             * http://www.ftchinese.com/tag/璀璨星耀
             * http://www.ftchinese.com/channel/chinabusinesswatch.html
             * http://www.ftchinese.com/channel/tradewar.html
             * http://www.ftchinese.com/m/corp/preview.html?pageid=huawei2018
             * http://www.ftchinese.com/m/marketing/Emotech2017.html
             * http://www.ftchinese.com/m/corp/preview.html?pageid=alibaba2018
             *
             * After clicked a link, it starts a ChannelActivity:
             * "meta": {
             *   "title": "璀璨星耀",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "5071",
             *   "adZone": "home/special/brightstar"
             *   }
             */
        ChannelSource(
            title = "特别报导",
            name = "news_special_report",
            contentPath = "/channel/special.html?webview=ftcapp&ad=no&001",
            htmlType = HTML_TYPE_COMPLETE),
        /**
             * "meta": {
             *   "title": "一周热门文章",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "5000",
             *   "adZone": "home/weekly"
             *   }
             */
            /**
             * "meta": {
             *   "title": "一周热门文章",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "5000",
             *   "adZone": "home/weekly"
             *   }
             */
        ChannelSource(
            title = "热门文章",
            name = "news_weekly",
            contentPath = "/channel/weekly.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * "meta": {
             *   "title": "数据新闻",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1100",
             *   "adZone": "home/datanews"
             *   }
             */
            /**
             * "meta": {
             *   "title": "数据新闻",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1100",
             *   "adZone": "home/datanews"
             *   }
             */
        ChannelSource(
            title = "数据新闻",
            name = "news_data",
            contentPath = "/channel/datanews.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
        /**
             * This page is loaded directly into a webview.
             * All articles on this page have to be handle
             * by WebViewClient.
             *
             * URL pattern:
             * http://www.ftchinese.com/m/corp/preview.html?pageid=2018af
             */
            /**
             * This page is loaded directly into a webview.
             * All articles on this page have to be handle
             * by WebViewClient.
             *
             * URL pattern:
             * http://www.ftchinese.com/m/corp/preview.html?pageid=2018af
             */
        ChannelSource(
            title = "会议活动",
            name = "news_events",
            contentPath = "/m/corp/preview.html?pageid=events&webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
        ChannelSource(
            title = "FT研究院",
            name = "news_fta",
            contentPath = "/m/corp/preview.html?pageid=fti&webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
        ChannelSource(
            title = "高端物业",
            name = "news_property",
            contentPath = "/m/corp/preview.html?pageid=property&webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT)
    )

    val englishPages = listOf(
        ChannelSource(
            title = "最新",
            name = "english_latest",
            contentPath = "/channel/english.html?webview=ftcapp&bodyonly=yes&001",
            htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "FT英语电台",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1100",
             *   "adZone": "english/radio"
             *   }
             *  [
             *      {
                        "id": "12800",
                        "type": "interactive",
                        "headline": "摘金奇缘：财富和快乐可以兼得吗？",
                        "shortlead":
                        "http://v.ftimg.net/album/8960edda-b102-11e8-8d14-6f049d06439c.mp3",
                        "subType": "radio"
                    }
             *  ]
             */
        ChannelSource(
            title = "英语电台",
            name = "english_radio",
            contentPath = "/channel/radio.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "金融英语速读",
                "description": "",
                "theme": "default",
                "adid": "1100",
                "adZone": "english/speedread"
                }
             */
        ChannelSource(
            "金融英语速读",
            name = "english_speedreading",
            contentPath = "/channel/speedread.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
            /**
             * [
            "title": "音乐之生",
            "api": "https://api003.ftmailbox.com/channel/json.html?pageid=speedread&dfadfadfadfadf",
            "listapi": "https://api003.ftmailbox.com/channel/lifeofasong.html?webview=ftcapp&bodyonly=yes",
            "url":"http://www.ftchinese.com/channel/lifeofasong.html?webview=ftcapp",
            "screenName":"english/learnenglish",
            "coverTheme": "",
            "new": "yes"
            ],
             */
        ChannelSource(
            title = "音乐之生",
            name = "english_music",
            contentPath = "/channel/lifeofasong.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT
        ),
        ChannelSource(
            title = "麦可林学英语",
            name = "english_mle",
            contentPath = "/m/corp/preview.html?pageid=learnenglish&webview=ftcapp",
            htmlType = HTML_TYPE_FRAGMENT
        ),
            /**
             * "meta": {
                "title": "双语阅读",
                "description": "",
                "theme": "default",
                "adid": "1100",
                "adZone": "english/ce"
                }
             */
        ChannelSource(
            title = "双语阅读",
            name = "english_bilingual",
            contentPath = "/channel/ce.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),

        ChannelSource(
            title = "每日一词",
            name = "english_daily_word",
            contentPath = "/channel/dailyword.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT
        ),

            /**
             * "meta": {
                "title": "原声视频",
                "description": "",
                "theme": "default",
                "adid": "1100",
                "adZone": "english/ev"
                }
             */
        ChannelSource(
            title = "原声视频",
            name = "english_video",
            contentPath = "/channel/ev.html?webview=ftcapp&bodyonly=yes&001",
            htmlType = HTML_TYPE_FRAGMENT)
    )

    val ftaPages = listOf(
            /**
             * "meta": {
                "title": "",
                "description": "",
                "theme": "default",
                "adid": "1100",
                "adZone": "home"
                }
             */
        ChannelSource(
            title = "商学院观察",
            name = "fta_story",
            contentPath = "/m/corp/preview.html?pageid=mbastory&webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "热点观察 - FT商学院",
                "description": "",
                "theme": "default",
                "adid": "1701",
                "adZone": "home"
                }
             */
        ChannelSource(
            title = "热点观察",
            name = "fta_hot",
            contentPath = "/m/corp/preview.html?pageid=hotcourse&webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "MBA训练营",
                "description": "",
                "theme": "default",
                "adid": "1701",
                "adZone": "management/mba"
                }
             */
        ChannelSource(
            title = "MBA训练营",
            name = "fta_gym",
            contentPath = "/channel/mbagym.html?webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "互动小测",
                "description": "",
                "theme": "default",
                "adid": "1701",
                "adZone": "home"
                }
             */
        ChannelSource(
            title = "互动小测",
            name = "fta_quiz",
            contentPath = "/m/corp/preview.html?pageid=quizplus&webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),
            /**
             * Article items for this page:
             * {
             *  "meta": {
             *      "title":"深度阅读 - FT商学院",
             *      "description":"",
             *      "theme":"default",
             *      "adid":"1701",
             *      "adZone":"home"
             * },
             * [
             *     {
             *          "id":"7457",
             *          "type":"interactive",
             *          "headline":"拆分北京",
             *          "subType":"mbagym"
             *      }
             * ]
             */
        ChannelSource(
            title = "深度阅读",
            name = "fta_reading",
            contentPath = "/m/corp/preview.html?pageid=mbaread&webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT)
    )

    val videoPages = listOf(
            /**
             * "meta": {
                "title": "视频",
                "description": "",
                "theme": "default",
                "adid": "1900",
                "adZone": "stream"
                }
             */
        ChannelSource(
            title = "最新",
            name = "video_latest",
            contentPath = "/channel/audiovideo.html?webview=ftcapp&bodyonly=yes&norepeat=yes",
            htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel.
             * Handle webUrl click in WebViewClient.
             */
        ChannelSource(
            title = "高端视点",
            name = "video_viewtop",
            contentPath = "/channel/viewtop.html?webview=ftcapp&bodyonly=yes&norepeat=yes",
            htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel.
             * Handle webUrl click in WebViewClient
             */
        ChannelSource(
            title = "麦可林学英语",
            name = "video_learn_english",
            contentPath = "/m/corp/preview.html?pageid=learnenglish&webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT),

        ChannelSource(
            title = "BoomEar艺术播客",
            name = "video_boomear",
            contentPath = "/m/corp/preview.html?pageid=boomear&webview=ftcapp&bodyonly=yes",
            htmlType = HTML_TYPE_FRAGMENT
        ),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel.
             * Handle webUrl click in WebViewClient
             */
        ChannelSource(
            title = "秒懂",
            name = "video_explain",
            contentPath = "/channel/explainer.html?webview=ftcapp&bodyonly=yes&norepeat=yes",
            htmlType = HTML_TYPE_FRAGMENT
        ),

            /**
             * No list in this channel
             * Handle webUrl click in WebViewClient
             */
        ChannelSource(
            title = "FT看见",
            name = "video_feature",
            contentPath = "/channel/vfeatures.html?webview=ftcapp&bodyonly=yes&norepeat=yes",
            htmlType = HTML_TYPE_FRAGMENT
        ),
            /**
             * No list in this channel
             * Handle webUrl click in WebViewClient
             */
        ChannelSource(
            title = "有色眼镜",
            name = "video_tinted",
            contentPath = "/channel/videotinted.html?webview=ftcapp&bodyonly=yes&norepeat=yes",
            htmlType = HTML_TYPE_FRAGMENT
        )
    )
}
