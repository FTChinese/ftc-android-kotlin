package com.ft.ftchinese.models

import com.ft.ftchinese.BuildConfig

const val HOST_FTC = "www.ftchinese.com"
const val HOST_MAILBOX = "api003.ftmailbox.com"
const val HOST_FTA = "www.ftacademy.cn"

const val URL_FTC = "http://www.ftchinese.com"
const val URL_MAILBOX = "https://api003.ftmailbox.com"

val hostNames = arrayOf(HOST_FTC, HOST_MAILBOX)

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
            PagerTab(
                    title = "首页",
                    name = "news_home",
                    contentUrl = "https://api003.ftmailbox.com/?webview=ftcapp&bodyonly=yes&maxB=1&backupfile=localbackup&showIAP=yes&pagetype=home&001&android=${BuildConfig.VERSION_CODE}",
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
             * WVClient will receive url like http://www.ftchinese.com/china.html?page=2
             * because this is a relative url and it is
             * relative to web view's base url.
             */
            PagerTab(title = "中国", name = "news_china", contentUrl = "https://api003.ftmailbox.com/channel/china.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "独家",
                "description": "",
                "theme": "default",
                "adid": "1100",
                "adZone": "home/exclusive"
                }
                [
                    {
                    "id": "001080064",
                    "type": "premium",
                    "headline": "投资者将追问软银与沙特关系"
                    }
                ]
             */
            PagerTab(title = "独家", name = "news_scoop", contentUrl = "https://api003.ftmailbox.com/channel/exclusive.html?webview=ftcapp&bodyonly=yes&ad=no&001", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "编辑精选",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1100",
             *   "adZone": "home/editorchoice"
             *   }
             * This page is not processed by JS. You have to intercept click on url and then open a ChannelActivity.
             * Links on this page looks like:
             * http://www.ftchinese.com/channel/editorchoice-issue.html?issue=EditorChoice-20181105
             * See ChannelWebViewClient.
             */
            PagerTab(title = "编辑精选", name = "news_editor_choice", contentUrl = "https://api003.ftmailbox.com/channel/editorchoice.html?webview=ftcapp&bodyonly=yes&ad=no&showEnglishAudio=yes&018", htmlType = HTML_TYPE_FRAGMENT),
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
            PagerTab(title = "全球", name = "news_global", contentUrl = "https://api003.ftmailbox.com/channel/world.html?webview=ftcapp&bodyonly=yes&002", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "观点",
                "description": "",
                "theme": "default",
                "adid": "1600",
                "adZone": "opinion"
                }
             */
            PagerTab(title = "观点", name = "news_opinions", contentUrl = "https://api003.ftmailbox.com/channel/opinion.html?webview=ftcapp&bodyonly=yes&ad=no", htmlType = HTML_TYPE_FRAGMENT),
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
            PagerTab(title = "专栏", name = "news_column", contentUrl = "https://api003.ftmailbox.com/channel/column.html?webview=ftcapp&bodyonly=yes&ad=no", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "金融市场",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1400",
             *   "adZone": "markets"
             *   }
             */
            PagerTab(title = "金融市场", name = "news_markets", contentUrl = "https://api003.ftmailbox.com/channel/markets.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "金融市场",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1400",
             *   "adZone": "markets"
             *   }
             */
            PagerTab(title = "商业", name = "news_business", contentUrl = "https://api003.ftmailbox.com/channel/markets.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "科技",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1502",
             *   "adZone": "business/technology"
             *   }
             */
            PagerTab(title = "科技", name = "news_technology", contentUrl = "https://api003.ftmailbox.com/channel/technology.html?webview=ftcapp&bodyonly=yes&001", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "教育",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "2200",
             *   "adZone": "education"
             *   }
             */
            PagerTab(title = "教育", name = "news_education", contentUrl = "https://api003.ftmailbox.com/channel/education.html?webview=ftcapp&bodyonly=yes&001", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "管理",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1700",
             *   "adZone": "management"
             *   }
             */
            PagerTab(title = "管理", name = "news_management", contentUrl = "https://api003.ftmailbox.com/channel/management.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "生活时尚",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1800",
             *   "adZone": "lifestyle"
             *   }
             */
            PagerTab(title = "生活时尚", name = "news_life_style", contentUrl = "https://api003.ftmailbox.com/channel/lifestyle.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
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
            PagerTab(title = "特别报导", name = "news_special_report", contentUrl = "http://www.ftchinese.com/channel/special.html?webview=ftcapp&ad=no&001", htmlType = HTML_TYPE_COMPLETE),
            /**
             * "meta": {
             *   "title": "一周热门文章",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "5000",
             *   "adZone": "home/weekly"
             *   }
             */
            PagerTab(title = "热门文章", name = "news_weekly", contentUrl = "https://api003.ftmailbox.com/channel/weekly.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
             *   "title": "数据新闻",
             *   "description": "",
             *   "theme": "default",
             *   "adid": "1100",
             *   "adZone": "home/datanews"
             *   }
             */
            PagerTab(title = "数据新闻", name = "news_data", contentUrl = "https://api003.ftmailbox.com/channel/datanews.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * This page is loaded directly into a webview.
             * All articles on this page have to be handle
             * by WebViewClient.
             *
             * URL pattern:
             * http://www.ftchinese.com/m/corp/preview.html?pageid=2018af
             */
            PagerTab(title = "会议活动", name = "news_events", contentUrl = "http://www.ftchinese.com/m/events/event.html?webview=ftcapp", htmlType = HTML_TYPE_COMPLETE),
            PagerTab(title = "FT研究院", name = "news_fta", contentUrl = "http://www.ftchinese.com/m/marketing/intelligence.html?webview=ftcapp&001", htmlType = HTML_TYPE_COMPLETE)
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
            PagerTab(title = "英语电台", name = "english_radio", contentUrl = "https://api003.ftmailbox.com/channel/radio.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "金融英语速读",
                "description": "",
                "theme": "default",
                "adid": "1100",
                "adZone": "english/speedread"
                }
             */
            PagerTab("金融英语速读", name = "english_speedreading", contentUrl = "https://api003.ftmailbox.com/channel/speedread.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "双语阅读",
                "description": "",
                "theme": "default",
                "adid": "1100",
                "adZone": "english/ce"
                }
             */
            PagerTab("双语阅读", name = "english_bilingual", contentUrl = "https://api003.ftmailbox.com/channel/ce.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "原声视频",
                "description": "",
                "theme": "default",
                "adid": "1100",
                "adZone": "english/ev"
                }
             */
            PagerTab("原声视频", name = "english_video", contentUrl = "https://api003.ftmailbox.com/channel/ev.html?webview=ftcapp&bodyonly=yes&001", htmlType = HTML_TYPE_FRAGMENT)
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
            PagerTab(title = "商学院观察", name = "fta_story", contentUrl = "https://api003.ftmailbox.com/m/corp/preview.html?pageid=mbastory&webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "热点观察 - FT商学院",
                "description": "",
                "theme": "default",
                "adid": "1701",
                "adZone": "home"
                }
             */
            PagerTab(title = "热点观察", name = "fta_hot", contentUrl = "https://api003.ftmailbox.com/m/corp/preview.html?pageid=hotcourse&webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "MBA训练营",
                "description": "",
                "theme": "default",
                "adid": "1701",
                "adZone": "management/mba"
                }
             */
            PagerTab(title = "MBA训练营", name = "fta_gym", contentUrl = "https://api003.ftmailbox.com/channel/mbagym.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "互动小测",
                "description": "",
                "theme": "default",
                "adid": "1701",
                "adZone": "home"
                }
             */
            PagerTab(title = "互动小测", name = "fta_quiz", contentUrl = "https://api003.ftmailbox.com/m/corp/preview.html?pageid=quizplus&webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
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
            PagerTab(title = "深度阅读", name = "fta_reading", contentUrl = "https://api003.ftmailbox.com/m/corp/preview.html?pageid=mbaread&webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT)
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
            PagerTab(title = "最新", name = "video_latest", contentUrl = "https://api003.ftmailbox.com/channel/stream.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel.
             * Handle url click in WebViewClient.
             */
            PagerTab(title = "政经", name = "video_politics", contentUrl = "https://api003.ftmailbox.com/channel/vpolitics.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel.
             * Handle url click in WebViewClient
             */
            PagerTab(title = "商业", name = "video_business", contentUrl = "https://api003.ftmailbox.com/channel/vbusiness.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel.
             * Handle url click in WebViewClient
             */
            PagerTab(title = "秒懂", name = "video_explain", contentUrl = "https://api003.ftmailbox.com/channel/explainer.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel
             * Handle url click in WebViewClient
             */
            PagerTab(title = "金融", name = "video_finance", contentUrl = "https://api003.ftmailbox.com/channel/vfinance.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * "meta": {
                "title": "FT中文网",
                "description": "",
                "theme": "default",
                "adid": "1000",
                "adZone": "home"
                }
             * No list in this channel
             * Handle url click in WebViewClient
             */
            PagerTab(title = "文化", name = "video_culture", contentUrl = "https://api003.ftmailbox.com/channel/vculture.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * No list in this channel
             * Handle url click in WebViewClient
             */
            PagerTab(title = "高端视点", name = "video_top", contentUrl = "http://www.ftchinese.com/channel/viewtop.html?webview=ftcapp&norepeat=no", htmlType = HTML_TYPE_COMPLETE),
            /**
             * No list in this channel
             * Handle url click in WebViewClient
             */
            PagerTab(title = "FT看见", name = "video_feature", contentUrl = "https://api003.ftmailbox.com/channel/vfeatures.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
            /**
             * No list in this channel
             * Handle url click in WebViewClient
             */
            PagerTab(title = "有色眼镜", name = "video_tinted", contentUrl = "https://api003.ftmailbox.com/channel/videotinted.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT)
    )
}