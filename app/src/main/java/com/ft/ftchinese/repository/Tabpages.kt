package com.ft.ftchinese.repository

import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.HTML_TYPE_COMPLETE
import com.ft.ftchinese.model.content.HTML_TYPE_FRAGMENT
import com.ft.ftchinese.model.reader.Permission

// TabPages supply the data for TagPagerAdapter under a BottomNavigationView item.
object TabPages {

    val newsPages = listOf(

        ChannelSource(
            title = "首页",
            name = "news_home",
            contentPath = "/?webview=ftcapp&bodyonly=yes&maxB=1&backupfile=localbackup&showIAP=yes&pagetype=home&001",
            path = "",
            query = "maxB=1&backupfile=localbackup&showIAP=yes&pagetype=home&001",
            htmlType = HTML_TYPE_FRAGMENT
        ),

        ChannelSource(
            title = "中国",
            name = "news_china",
            contentPath = "/channel/china.html?webview=ftcapp&bodyonly=yes",
            path = "/channel/china.html",
            query = "",
            htmlType = HTML_TYPE_FRAGMENT),

        ChannelSource(
            title = "标准订阅",
            name = "news_standard_only",
            contentPath = "/channel/standardsubscription.html?webview=ftcapp&bodyonly=yes&ad=no&001",
            path = "/channel/standardsubscription.html",
            query = "ad=no&001",
            htmlType = HTML_TYPE_FRAGMENT,
            permission = Permission.STANDARD),

        ChannelSource(
            title = "高端订阅",
            name = "news_premium_only",
            contentPath = "/channel/premiumsubscription.html?webview=ftcapp&bodyonly=yes&ad=no&showEnglishAudio=yes&018",
            path = "/channel/premiumsubscription.html",
            query = "ad=no&showEnglishAudio=yes&018",
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
            path = "/channel/world.html",
            query = "002",
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
            path = "/channel/opinion.html",
            query = "ad=no",
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
            path = "/channel/column.html",
            query = "ad=no",
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
            path = "/channel/markets.html",
            query = "",
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
            path = "/channel/business.html",
            query = "",
            htmlType = HTML_TYPE_FRAGMENT),
        ChannelSource(
            title = "经济",
            name = "news_economy",
            contentPath = "/channel/economy.html?webview=ftcapp&bodyonly=yes&001",
            path = "/channel/economy.html",
            query = "001",
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
            path = "/channel/technology.html",
            query = "001",
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
            path = "/channel/education.html",
            query = "001",
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
            path = "/channel/management.html",
            query = "",
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
            path = "/channel/lifestyle.html",
            query = "",
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
            path = "/channel/special.html",
            query = "ad=no&001",
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
            path = "/channel/weekly.html",
            query = "",
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
            path = "/channel/datanews.html",
            query = "",
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
            path = "/m/corp/preview.html",
            query = "pageid=events",
            htmlType = HTML_TYPE_FRAGMENT),
        ChannelSource(
            title = "FT研究院",
            name = "news_fta",
            contentPath = "/m/corp/preview.html?pageid=fti&webview=ftcapp&bodyonly=yes",
            path = "/m/corp/preview.html",
            query = "pageid=fti",
            htmlType = HTML_TYPE_FRAGMENT),
        ChannelSource(
            title = "高端物业",
            name = "news_property",
            contentPath = "/m/corp/preview.html?pageid=property&webview=ftcapp&bodyonly=yes",
            path = "/m/corp/preview.html",
            query = "pageid=property",
            htmlType = HTML_TYPE_FRAGMENT)
    )

    val englishPages = listOf(
        ChannelSource(
            title = "最新",
            name = "english_latest",
            contentPath = "/channel/english.html?webview=ftcapp&bodyonly=yes&001",
            path = "/channel/english.html",
            query = "001",
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
            path = "/channel/radio.html",
            query = "",
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
            path = "/channel/speedread.html",
            query = "",
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
            path = "/channel/lifeofasong.html",
            query = "",
            htmlType = HTML_TYPE_FRAGMENT
        ),
        ChannelSource(
            title = "麦可林学英语",
            name = "english_mle",
            contentPath = "/m/corp/preview.html?pageid=learnenglish&webview=ftcapp",
            path = "/m/corp/preview.html",
            query = "pageid=learnenglish",
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
            path = "/channel/ce.html",
            query = "",
            htmlType = HTML_TYPE_FRAGMENT),

        ChannelSource(
            title = "每日一词",
            name = "english_daily_word",
            contentPath = "/channel/dailyword.html?webview=ftcapp&bodyonly=yes",
            path = "/channel/dailyword.html",
            query = "",
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
            path = "/channel/ev.html",
            query = "001",
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
            path = "/m/corp/preview.html",
            query = "pageid=mbastory",
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
            path = "/m/corp/preview.html",
            query = "pageid=hotcourse",
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
            path = "/channel/mbagym.html",
            query = "",
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
            path = "/m/corp/preview.html",
            query = "pageid=quizplus",
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
            path = "/m/corp/preview.html",
            query = "pageid=mbaread",
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
            path = "/channel/audiovideo.html",
            query = "norepeat=yes",
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
            path = "/channel/viewtop.html",
            query = "norepeat=yes",
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
            path = "/m/corp/preview.html",
            query = "pageid=learnenglish",
            htmlType = HTML_TYPE_FRAGMENT),

        ChannelSource(
            title = "BoomEar艺术播客",
            name = "video_boomear",
            contentPath = "/m/corp/preview.html?pageid=boomear&webview=ftcapp&bodyonly=yes",
            path = "/m/corp/preview.html",
            query = "pageid=boomear",
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
            path = "/channel/explainer.html",
            query = "norepeat=yes",
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
            path = "/channel/vfeatures.html",
            query = "norepeat=yes",
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
            path = "/channel/videotinted.html",
            query = "norepeat=yes",
            htmlType = HTML_TYPE_FRAGMENT
        )
    )
}
