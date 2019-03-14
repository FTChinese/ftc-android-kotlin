package com.ft.ftchinese.models

import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.util.FTC_OFFICIAL_URL
import com.ft.ftchinese.util.MAILBOX_URL

object Navigation {

    val newsPages = arrayOf(
            /**
             * "meta": {
             *   "title": "FT中文网",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1000",
             *   "adZone": "home"
             *  }
             *  [
             *      {
                    "id": "001080012",
                    "type": "story",
                    "headline": "与让-保罗•高提耶共进午餐",
                    "eaudio":
                    "https://s3-us-west-2.amazonaws.com/ftlabs-audio-rss-bucket.prod/6f0390b0-a549-11e8-8ecf-a7ae1beff35b.mp3"
                    },
                    {
                    "id": "001080060",
                    "type": "premium",
                    "headline": "哈佛商学院案例教学法的利与弊",
                    "eaudio":
                    "https://s3-us-west-2.amazonaws.com/ftlabs-audio-rss-bucket.prod/0b1aeb22-d765-11e8-a854-33d6f82e62f8.mp3"
                    },
                    {
                    "id": "12796",
                    "type": "interactive",
                    "headline": "瑞士新疗法令截瘫患者重新行走",
                    "eaudio":
                    "http://v.ftimg.net/album/b4ab2028-dc63-11e8-9f04-38d397e6661c.mp3",
                    "subType": "speedreading"
                    },
                    {
                    "id": "12794",
                    "type": "interactive",
                    "headline": "巴尔默的17亿美元给了谁？",
                    "shortlead":
                    "http://v.ftimg.net/album/80adbbbe-68d7-11e8-aee1-39f3459514fd.mp3",
                    "subType": "radio"
                    }
             *  ]
             */
            ChannelSource(
                    title = "首页",
                    name = "news_home",
                    contentUrl = "$MAILBOX_URL/?webview=ftcapp&bodyonly=yes&maxB=1&backupfile=localbackup&showIAP=yes&pagetype=home&001&android=${BuildConfig.VERSION_CODE}",
                    htmlType = HTML_TYPE_FRAGMENT
            ),
            /**
             * "meta": {
                    "title": "中国",
                    "description": "",
                    "theme": "default",
                    "adid": "1100",
                    "adZone": "china"
                }
                [
                    {
                    "id": "001080053",
                    "type": "story",
                    "headline": "香港拟对加密货币交易实施监管",
                    "timeStamp": "1541001600"
                    }
                ]
             * Pagination: china.html?page=2
             * When user clicked such kind of links,
             * WVClient will receive webUrl like http://www.ftchinese.com/china.html?page=2
             * because this is a relative webUrl and it is
             * relative to web view's base webUrl.
             */
            ChannelSource(
                    title = "中国",
                    name = "news_china",
                    contentUrl = "$MAILBOX_URL/channel/china.html?webview=ftcapp&bodyonly=yes",
                    htmlType = HTML_TYPE_FRAGMENT),
            ChannelSource(
                    title = "标准订阅",
                    name = "news_standard_only",
                    contentUrl = "$MAILBOX_URL/channel/standardsubscription.html?webview=ftcapp&bodyonly=yes&ad=no&001",
                    htmlType = HTML_TYPE_FRAGMENT,
                    requiredTier = Tier.STANDARD),
            ChannelSource(
                    title = "高端订阅",
                    name = "news_premium_only",
                    contentUrl = "$MAILBOX_URL/channel/premiumsubscription.html?webview=ftcapp&bodyonly=yes&ad=no&showEnglishAudio=yes&018",
                    htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "编辑精选",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1100",
             *   "adZone": "home/editorchoice"
             *   }
             * This page is not processed by JS. You have to intercept click on webUrl and then open a ChannelActivity.
             * Links on this page looks like:
             * http://www.ftchinese.com/channel/editorchoice-issue.html?issue=EditorChoice-20181105
             * See ChannelWebViewClient.
             */
            ChannelSource(
                    title = "编辑精选",
                    name = "news_editor_choice",
                    contentUrl = "$MAILBOX_URL/channel/editorchoice.html?webview=ftcapp&bodyonly=yes&ad=no&showEnglishAudio=yes&018",
                    htmlType = HTML_TYPE_FRAGMENT),
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
                    contentUrl = "$MAILBOX_URL/channel/world.html?webview=ftcapp&bodyonly=yes&002",
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
            ChannelSource(
                    title = "观点",
                    name = "news_opinions",
                    contentUrl = "$MAILBOX_URL/channel/opinion.html?webview=ftcapp&bodyonly=yes&ad=no",
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
            ChannelSource(title = "专栏", name = "news_column", contentUrl = "$MAILBOX_URL/channel/column.html?webview=ftcapp&bodyonly=yes&ad=no", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "金融市场",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1400",
             *   "adZone": "markets"
             *   }
             */
            ChannelSource(title = "金融市场", name = "news_markets", contentUrl = "$MAILBOX_URL/channel/markets.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
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
                    contentUrl = "$MAILBOX_URL/channel/business.html?webview=ftcapp&bodyonly=yes",
                    htmlType = HTML_TYPE_FRAGMENT),
            ChannelSource(
                    title = "经济",
                    name = "news_economy",
                    contentUrl = "$MAILBOX_URL/channel/economy.html?webview=ftcapp&bodyonly=yes&001",
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
            ChannelSource(
                    title = "科技",
                    name = "news_technology",
                    contentUrl = "$MAILBOX_URL/channel/technology.html?webview=ftcapp&bodyonly=yes&001",
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
            ChannelSource(
                    title = "教育",
                    name = "news_education",
                    contentUrl = "$MAILBOX_URL/channel/education.html?webview=ftcapp&bodyonly=yes&001",
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
            ChannelSource(
                    title = "管理",
                    name = "news_management",
                    contentUrl = "$MAILBOX_URL/channel/management.html?webview=ftcapp&bodyonly=yes",
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
            ChannelSource(
                    title = "生活时尚",
                    name = "news_life_style",
                    contentUrl = "$MAILBOX_URL/channel/lifestyle.html?webview=ftcapp&bodyonly=yes",
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
            ChannelSource(
                    title = "特别报导",
                    name = "news_special_report",
                    contentUrl = "$FTC_OFFICIAL_URL/channel/special.html?webview=ftcapp&ad=no&001",
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
            ChannelSource(
                    title = "热门文章",
                    name = "news_weekly",
                    contentUrl = "$MAILBOX_URL/channel/weekly.html?webview=ftcapp&bodyonly=yes",
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
            ChannelSource(
                    title = "数据新闻",
                    name = "news_data",
                    contentUrl = "$MAILBOX_URL/channel/datanews.html?webview=ftcapp&bodyonly=yes",
                    htmlType = HTML_TYPE_FRAGMENT),
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
                    contentUrl = "$FTC_OFFICIAL_URL/m/corp/preview.html?pageid=events&webview=ftcapp&bodyonly=yes",
                    htmlType = HTML_TYPE_FRAGMENT),
            ChannelSource(
                    title = "FT研究院",
                    name = "news_fta",
                    contentUrl = "$MAILBOX_URL/m/corp/preview.html?pageid=fti&webview=ftcapp&bodyonly=yes",
                    htmlType = HTML_TYPE_FRAGMENT),
            ChannelSource(
                    title = "高端物业",
                    name = "news_property",
                    contentUrl = "$MAILBOX_URL/m/corp/preview.html?pageid=property&webview=ftcapp&bodyonly=yes",
                    htmlType = HTML_TYPE_FRAGMENT)
    )

    val englishPages = arrayOf(
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
            ChannelSource(title = "英语电台", name = "english_radio", contentUrl = "$MAILBOX_URL/channel/radio.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "金融英语速读",
                "description": "",
                "theme": "default",
                "adid": "1100",
                "adZone": "english/speedread"
                }
             */
            ChannelSource("金融英语速读", name = "english_speedreading", contentUrl = "$MAILBOX_URL/channel/speedread.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
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
                    "双语阅读",
                    name = "english_bilingual",
                    contentUrl = "$MAILBOX_URL/channel/ce.html?webview=ftcapp&bodyonly=yes",
                    htmlType = HTML_TYPE_FRAGMENT),
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
                    "原声视频",
                    name = "english_video",
                    contentUrl = "$MAILBOX_URL/channel/ev.html?webview=ftcapp&bodyonly=yes&001",
                    htmlType = HTML_TYPE_FRAGMENT)
    )

    val ftaPages = arrayOf(
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
                    contentUrl = "$MAILBOX_URL/m/corp/preview.html?pageid=mbastory&webview=ftcapp&bodyonly=yes",
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
                    contentUrl = "$MAILBOX_URL/m/corp/preview.html?pageid=hotcourse&webview=ftcapp&bodyonly=yes",
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
                    contentUrl = "$MAILBOX_URL/channel/mbagym.html?webview=ftcapp&bodyonly=yes",
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
                    contentUrl = "$MAILBOX_URL/m/corp/preview.html?pageid=quizplus&webview=ftcapp&bodyonly=yes",
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
                    contentUrl = "$MAILBOX_URL/m/corp/preview.html?pageid=mbaread&webview=ftcapp&bodyonly=yes",
                    htmlType = HTML_TYPE_FRAGMENT)
    )

    val videoPages = arrayOf(
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
                    contentUrl = "$MAILBOX_URL/channel/stream.html?webview=ftcapp&bodyonly=yes&norepeat=yes",
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
                    title = "政经",
                    name = "video_politics",
                    contentUrl = "$MAILBOX_URL/channel/vpolitics.html?webview=ftcapp&bodyonly=yes&norepeat=yes",
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
                    title = "商业",
                    name = "video_business",
                    contentUrl = "$MAILBOX_URL/channel/vbusiness.html?webview=ftcapp&bodyonly=yes&norepeat=yes",
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
            ChannelSource(title = "秒懂", name = "video_explain", contentUrl = "$MAILBOX_URL/channel/explainer.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel
             * Handle webUrl click in WebViewClient
             */
            ChannelSource(title = "金融", name = "video_finance", contentUrl = "$MAILBOX_URL/channel/vfinance.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel
             * Handle webUrl click in WebViewClient
             */
            ChannelSource(title = "文化", name = "video_culture", contentUrl = "$MAILBOX_URL/channel/vculture.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * No list in this channel
             * Handle webUrl click in WebViewClient
             */
            ChannelSource(title = "高端视点", name = "video_top", contentUrl = "$MAILBOX_URL/channel/viewtop.html?webview=ftcapp&norepeat=no", htmlType = HTML_TYPE_COMPLETE),
            /**
             * No list in this channel
             * Handle webUrl click in WebViewClient
             */
            ChannelSource(title = "FT看见", name = "video_feature", contentUrl = "$MAILBOX_URL/channel/vfeatures.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * No list in this channel
             * Handle webUrl click in WebViewClient
             */
            ChannelSource(title = "有色眼镜", name = "video_tinted", contentUrl = "$MAILBOX_URL/channel/videotinted.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT)
    )
}