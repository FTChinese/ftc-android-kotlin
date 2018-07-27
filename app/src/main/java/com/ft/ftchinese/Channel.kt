package com.ft.ftchinese

class Endpoints{
    companion object {
        const val HOST_FTC = "www.ftchinese.com"
        const val HOST_MAILBOX = "api003.ftmailbox.com"
        val hosts = arrayOf(HOST_FTC, HOST_MAILBOX)
    }
}

/**
 * ListPage contains the data used by a page in ViewPager
 */
data class ListPage (
        val title: String, // A Tab's title
        val name: String,  // Cache filename used by this tab
        val listUrl: String? = null, // Where to fetch data to be loaded in WebView.loadDataWith
        val webUrl: String? = null // A complete html page to be loaded directly into a WebView
) {
    companion object {
        val newsPages = arrayOf(
                ListPage(title = "首页", name = "news_frontpage", listUrl = "https://api003.ftmailbox.com/?webview=ftcapp&bodyonly=yes&maxB=1&backupfile=localbackup&showIAP=yes&pagetype=home&001"),
                ListPage(title = "中国", name = "news_china", listUrl = "https://api003.ftmailbox.com/channel/china.html?webview=ftcapp&bodyonly=yes"),
                ListPage(title = "独家", name = "news_scoop", listUrl = "https://api003.ftmailbox.com/channel/exclusive.html?webview=ftcapp&bodyonly=yes&ad=no&001"),
                ListPage(title = "编辑精选", name = "news_editor_choice", listUrl =  "https://api003.ftmailbox.com/channel/editorchoice.html?webview=ftcapp&bodyonly=yes&ad=no&showEnglishAudio=yes&018"),
                ListPage(title = "全球", name = "news_global", listUrl = "https://api003.ftmailbox.com/channel/world.html?webview=ftcapp&bodyonly=yes&002"),
                ListPage(title = "观点", name = "news_opinions", listUrl =  "https://api003.ftmailbox.com/channel/opinion.html?webview=ftcapp&bodyonly=yes&ad=no"),
                ListPage(title = "专栏", name = "news_column", listUrl =  "https://api003.ftmailbox.com/channel/column.html?webview=ftcapp&bodyonly=yes&ad=no"),
                ListPage(title = "金融市场", name = "news_markets", listUrl =  "https://api003.ftmailbox.com/channel/markets.html?webview=ftcapp&bodyonly=yes"),
                ListPage(title = "商业", name = "news_business", listUrl =  "https://api003.ftmailbox.com/channel/markets.html?webview=ftcapp&bodyonly=yes"),
                ListPage(title = "科技", name = "news_technology", listUrl =  "https://api003.ftmailbox.com/channel/technology.html?webview=ftcapp&bodyonly=yes&001"),
                ListPage(title = "教育", name = "news_education", listUrl =  "https://api003.ftmailbox.com/channel/education.html?webview=ftcapp&bodyonly=yes&001"),
                ListPage(title = "管理", name = "news_management", listUrl =  "https://api003.ftmailbox.com/channel/management.html?webview=ftcapp&bodyonly=yes"),
                ListPage(title = "生活时尚", name = "news_life_style", listUrl =  "https://api003.ftmailbox.com/channel/lifestyle.html?webview=ftcapp&bodyonly=yes"),
                ListPage(title = "特别报导", name = "news_special_report", webUrl = "http://www.ftchinese.com/channel/special.html?webview=ftcapp&ad=no&001"),
                ListPage(title = "热门文章", name = "news_top_stories", listUrl =  "https://api003.ftmailbox.com/channel/weekly.html?webview=ftcapp&bodyonly=yes"),
                ListPage(title = "数据新闻", name = "news_data", listUrl =  "https://api003.ftmailbox.com/channel/datanews.html?webview=ftcapp&bodyonly=yes"),
                ListPage(title = "会议活动", name = "news_events", webUrl = "http://www.ftchinese.com/m/events/event.html?webview=ftcapp"),
                ListPage(title = "FT研究院", name = "news_fta", webUrl = "http://www.ftchinese.com/m/marketing/intelligence.html?webview=ftcapp&001")
        )

        val englishPages = arrayOf(
                ListPage(title = "英语电台", name = "english_radio", listUrl =  "https://api003.ftmailbox.com/channel/radio.html?webview=ftcapp&bodyonly=yes"),
                ListPage("金融英语速读", name = "english_finance", listUrl = "https://api003.ftmailbox.com/channel/speedread.html?webview=ftcapp&bodyonly=yes"),
                ListPage("双语阅读", name = "english_bilingual", listUrl =  "https://api003.ftmailbox.com/channel/ce.html?webview=ftcapp&bodyonly=yes"),
                ListPage("原声视频", name = "english_video", listUrl =  "https://api003.ftmailbox.com/channel/ev.html?webview=ftcapp&bodyonly=yes&001")
        )

        val ftaPages = arrayOf(
                ListPage(title = "商学院观察", name = "fta_story", listUrl =  "https://api003.ftmailbox.com/m/corp/preview.html?pageid=mbastory&webview=ftcapp&bodyonly=yes"),
                ListPage(title = "热点观察", name = "fta_hot", listUrl =   "https://api003.ftmailbox.com/m/corp/preview.html?pageid=hotcourse&webview=ftcapp&bodyonly=yes"),
                ListPage(title = "MBA训练营", name = "fta_gym", listUrl =  "https://api003.ftmailbox.com/channel/mbagym.html?webview=ftcapp&bodyonly=yes"),
                ListPage(title = "互动小测", name = "fta_quiz", listUrl =  "https://api003.ftmailbox.com/m/corp/preview.html?pageid=quizplus&webview=ftcapp&bodyonly=yes"),
                ListPage(title = "深度阅读", name = "fta_reading", listUrl =  "https://api003.ftmailbox.com/m/corp/preview.html?pageid=mbaread&webview=ftcapp&bodyonly=yes")
        )

        val videoPages = arrayOf(
                ListPage(title = "最新", name = "video_latest", listUrl =  "https://api003.ftmailbox.com/channel/stream.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
                ListPage(title = "政经", name = "video_politics", listUrl =  "https://api003.ftmailbox.com/channel/vpolitics.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
                ListPage(title = "商业", name = "video_business", listUrl =  "https://api003.ftmailbox.com/channel/vbusiness.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
                ListPage(title = "秒懂", name = "video_explain", listUrl =  "https://api003.ftmailbox.com/channel/explainer.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
                ListPage(title = "金融", name = "video_finance", listUrl =  "https://api003.ftmailbox.com/channel/vfinance.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
                ListPage(title = "文化", name = "video_culture", listUrl =  "https://api003.ftmailbox.com/channel/vculture.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
                ListPage(title = "高端视点", name = "video_top",  webUrl = "http://www.ftchinese.com/channel/viewtop.html?webview=ftcapp&norepeat=no"),
                ListPage(title = "FT看见", name = "video_feature", listUrl =  "https://api003.ftmailbox.com/channel/vfeatures.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
                ListPage(title = "有色眼镜", name = "video_tinted", listUrl =  "https://api003.ftmailbox.com/channel/videotinted.html?webview=ftcapp&bodyonly=yes&norepeat=yes")
        )
    }
}

/**
 * The following data types is used to parse JSON data passed from WebView by WebAppInterface.postItems methods.
 * Most of them are useless, serving as placeholders so that we can extract the deep nested JSON values.
 * `ChannelMeta` represent the `meta` filed in JSON data.
 */
data class ChannelMeta(
        val title: String,
        val description: String,
        val theme: String,
        val adid: String
)

/**
 * This is the data type passed to ContentActivity so that it know what kind of data to load.
 * iOS equivalent might be Page/Layouts/Content/ContentItem.swift#ContentItem
 * The fields are collected from all HTML elements `div.item-container-app`.
 * See https://github.com/FTChinese/android-client/app/scripts/list.js.
 * In short it used those attributes:
 * `data-id` for `id`
 * `data-type` for type. Possible values: `story`, `interactive`,
 * The content of `a.item-headline-link` inside `div.item-container-app` for `headline`
 * `data-audio` for `shortlead`
 * `data-caudio` for `caudio`
 * `data-eaudio` for `eaudio`
 * `data-sub-type` for `subType`. Possible values: `radio`, `speedreading`
 * `data-date` for `timeStamp`
 *
 * NOTE: This is a terrible way of handling data. In the future we will provide a unified JSON API with clear naming style.
 */
data class ChannelItem(
        val id: String,
        val type: String,
        val subType: String? = null,
        val headline: String,
        val shortlead: String
) {
    var adId: String = ""

    val commentsId: String
        get() {
            return when(subType) {
                "interactive" -> "r_interactive_$id"
                "video" -> "r_video_$id"
                "story" -> id
                "photo", "photonews" -> "r_photo_$id"
                else -> "r_${type}_$id"
            }
        }

    val commentsOrder: String
        get() {
            return "story"
        }

    // See Page/FTChinese/Main/APIs.swift
    // https://api003.ftmailbox.com/interactive/12339?bodyonly=no&webview=ftcapp&001&exclusive&hideheader=yes&ad=no&inNavigation=yes&for=audio&enableScript=yes&v=24
    val apiUrl: String?
        get() {
            return when(type) {
                "story", "premium" -> "https://api.ftmailbox.com/index.php/jsapi/get_story_more_info/$id"
                "interactive" -> "https://api003.ftmailbox.com/interactive/$id?bodyonly=no&webview=ftcapp&001&exclusive&hideheader=yes&ad=no&inNavigation=yes&for=audio&enableScript=yes&v=24"
                "gym", "special" -> "https://api003.ftmailbox.com/$type/$id?bodyonly=yes&webview=ftcapp"
                "video" -> "https://api003.ftmailbox.com/$type/$id?bodyonly=yes&webview=ftcapp&004"
                "radio" -> "https://api003.ftmailbox.com/$type/$id?bodyonly=yes&webview=ftcapp&exclusive"
                else -> null
            }
        }
}

data class ChannelList(
        val items: Array<ChannelItem>
)
data class ChannelSection(
        val lists: Array<ChannelList>
)
data class ChannelContent(
        val meta: ChannelMeta,
        val sections: Array<ChannelSection>
)

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

//val sepcailUrl = mapOf(
//        "12073" to "http://www.ftchinese.com/channel/chinabusinesswatch.html",
//        "11977" to "http://www.ftchinese.com/tag/新经济公司IPO",
//        "11869" to "http://www.ftchinese.com/tag/引领变局之势",
//        "11041" to "http://www.ftchinese.com/channel/viewtop.html",
//        "11452" to "http://www.ftchinese.com/tag/2018中国女性力量",
//        "11364" to "http://www.ftchinese.com/m/marketing/Emotech2017.html",
//        "11205" to "https://api003.ftmailbox.com/tag/创新经济-物流4.0",
//        "11075" to "https://api003.ftmailbox.com/tag/经济发展新动力",
//        "10719" to "https://api003.ftmailbox.com/tag/2017年度报告",
//        "10399" to "https://api003.ftmailbox.com/tag/交通银行记账式原油",
//        "10295" to "http://www.ftchinese.com/m/marketing/antfinancial.html",
//        "10089" to "http://www.ftchinese.com/channel/teawithft.html",
//        "9911" to "https://api003.ftmailbox.com/tag/金砖五国峰会",
//        "9378" to "https://api003.ftmailbox.com/tag/智慧城市群",
//        "9355" to "http://www.ftchinese.com/m/marketing/creditease.html",
//        "9209" to "http://www.ftchinese.com/m/marketing/Emotech2017.html"
//)