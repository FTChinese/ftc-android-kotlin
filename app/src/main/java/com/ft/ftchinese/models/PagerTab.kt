package com.ft.ftchinese.models

import android.content.Context
import android.content.res.Resources
import com.ft.ftchinese.R
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.Store
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * PagerTab contains the data used by a page in ViewPager
 */
data class PagerTab (
        val title: String, // A Tab's title
        val name: String,  // Cache filename used by this tab
        val contentUrl: String,
        val htmlType: Int // Flag used to tell whether the url should be loaded directly
) : AnkoLogger {
    suspend fun htmlFromCache(context: Context?): String? {
        val job = async {
            Store.load(context, "$name.html")
        }

        return job.await()
    }

    fun fragmentFromCache(context: Context?): Deferred<String?> = async {
        Store.load(context, "$name.html")
    }

    /**
     * Crawl a web page and save it.
     */
    fun crawlWebAsync(context: Context?): Deferred<String?> = async {
        val htmlStr = Fetch().get(contentUrl).string()

        Store.save(context, "$name.html", htmlStr)

        htmlStr
    }

    fun render(template: String?, listContent: String?): String? {
        if (template == null || listContent == null) {
            return null
        }
        return template.replace("{list-content}", listContent)
                .replace("{{googletagservices-js}}", "")
    }

    companion object {
        // Indicate you need to craw an HTML fragment
        const val HTML_TYPE_FRAGMENT = 1
        // Indicate you need to load a complete web page into webview.
        const val HTML_TYPE_COMPLETE = 2

        fun readTemplate(resources: Resources): Deferred<String?> = async {
            Store.readRawFile(resources, R.raw.list)
        }

        val newsPages = arrayOf(
                PagerTab(title = "首页", name = "news_home", contentUrl = "https://api003.ftmailbox.com/?webview=ftcapp&bodyonly=yes&maxB=1&backupfile=localbackup&showIAP=yes&pagetype=home&001", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "中国", name = "news_china", contentUrl = "https://api003.ftmailbox.com/channel/china.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "独家", name = "news_scoop", contentUrl = "https://api003.ftmailbox.com/channel/exclusive.html?webview=ftcapp&bodyonly=yes&ad=no&001", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "编辑精选", name = "news_editor_choice", contentUrl = "https://api003.ftmailbox.com/channel/editorchoice.html?webview=ftcapp&bodyonly=yes&ad=no&showEnglishAudio=yes&018", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "全球", name = "news_global", contentUrl = "https://api003.ftmailbox.com/channel/world.html?webview=ftcapp&bodyonly=yes&002", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "观点", name = "news_opinions", contentUrl = "https://api003.ftmailbox.com/channel/opinion.html?webview=ftcapp&bodyonly=yes&ad=no", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "专栏", name = "news_column", contentUrl = "https://api003.ftmailbox.com/channel/column.html?webview=ftcapp&bodyonly=yes&ad=no", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "金融市场", name = "news_markets", contentUrl = "https://api003.ftmailbox.com/channel/markets.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "商业", name = "news_business", contentUrl = "https://api003.ftmailbox.com/channel/markets.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "科技", name = "news_technology", contentUrl = "https://api003.ftmailbox.com/channel/technology.html?webview=ftcapp&bodyonly=yes&001", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "教育", name = "news_education", contentUrl = "https://api003.ftmailbox.com/channel/education.html?webview=ftcapp&bodyonly=yes&001", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "管理", name = "news_management", contentUrl = "https://api003.ftmailbox.com/channel/management.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "生活时尚", name = "news_life_style", contentUrl = "https://api003.ftmailbox.com/channel/lifestyle.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "特别报导", name = "news_special_report", contentUrl = "http://www.ftchinese.com/channel/special.html?webview=ftcapp&ad=no&001", htmlType = HTML_TYPE_COMPLETE),
                PagerTab(title = "热门文章", name = "news_top_stories", contentUrl = "https://api003.ftmailbox.com/channel/weekly.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "数据新闻", name = "news_data", contentUrl = "https://api003.ftmailbox.com/channel/datanews.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "会议活动", name = "news_events", contentUrl = "http://www.ftchinese.com/m/events/event.html?webview=ftcapp", htmlType = HTML_TYPE_COMPLETE),
                PagerTab(title = "FT研究院", name = "news_fta", contentUrl = "http://www.ftchinese.com/m/marketing/intelligence.html?webview=ftcapp&001", htmlType = HTML_TYPE_COMPLETE)
        )

        val englishPages = arrayOf(
                PagerTab(title = "英语电台", name = "english_radio", contentUrl = "https://api003.ftmailbox.com/channel/radio.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab("金融英语速读", name = "english_finance", contentUrl = "https://api003.ftmailbox.com/channel/speedread.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab("双语阅读", name = "english_bilingual", contentUrl = "https://api003.ftmailbox.com/channel/ce.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab("原声视频", name = "english_video", contentUrl = "https://api003.ftmailbox.com/channel/ev.html?webview=ftcapp&bodyonly=yes&001", htmlType = HTML_TYPE_FRAGMENT)
        )

        val ftaPages = arrayOf(
                PagerTab(title = "商学院观察", name = "fta_story", contentUrl = "https://api003.ftmailbox.com/m/corp/preview.html?pageid=mbastory&webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "热点观察", name = "fta_hot", contentUrl = "https://api003.ftmailbox.com/m/corp/preview.html?pageid=hotcourse&webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "MBA训练营", name = "fta_gym", contentUrl = "https://api003.ftmailbox.com/channel/mbagym.html?webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "互动小测", name = "fta_quiz", contentUrl = "https://api003.ftmailbox.com/m/corp/preview.html?pageid=quizplus&webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "深度阅读", name = "fta_reading", contentUrl = "https://api003.ftmailbox.com/m/corp/preview.html?pageid=mbaread&webview=ftcapp&bodyonly=yes", htmlType = HTML_TYPE_FRAGMENT)
        )

        val videoPages = arrayOf(
                PagerTab(title = "最新", name = "video_latest", contentUrl = "https://api003.ftmailbox.com/channel/stream.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "政经", name = "video_politics", contentUrl = "https://api003.ftmailbox.com/channel/vpolitics.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "商业", name = "video_business", contentUrl = "https://api003.ftmailbox.com/channel/vbusiness.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "秒懂", name = "video_explain", contentUrl = "https://api003.ftmailbox.com/channel/explainer.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "金融", name = "video_finance", contentUrl = "https://api003.ftmailbox.com/channel/vfinance.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "文化", name = "video_culture", contentUrl = "https://api003.ftmailbox.com/channel/vculture.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "高端视点", name = "video_top", contentUrl = "http://www.ftchinese.com/channel/viewtop.html?webview=ftcapp&norepeat=no", htmlType = HTML_TYPE_COMPLETE),
                PagerTab(title = "FT看见", name = "video_feature", contentUrl = "https://api003.ftmailbox.com/channel/vfeatures.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT),
                PagerTab(title = "有色眼镜", name = "video_tinted", contentUrl = "https://api003.ftmailbox.com/channel/videotinted.html?webview=ftcapp&bodyonly=yes&norepeat=yes", htmlType = HTML_TYPE_FRAGMENT)
        )
    }
}