package com.ft.ftchinese.models

import android.content.Context
import android.content.res.Resources
import com.ft.ftchinese.R
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.Store
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger

/**
 * PagerTab contains the data used by a page in ViewPager
 */
data class PagerTab (
        val title: String, // A Tab's title
        val name: String,  // Cache filename used by this tab
        val contentUrl: String,
        val htmlType: Int // Flag used to tell whether the url should be loaded directly
) : AnkoLogger {

    fun fragmentFromCache(context: Context?): String? {
        return Store.load(context, "$name.html")
    }

    /**
     * Crawl a web page and save it.
     */
    fun crawlWeb(context: Context?):String? {
        val htmlStr = Fetch().get(contentUrl).string()

        GlobalScope.launch {
            Store.save(context, "$name.html", htmlStr)
        }

        return htmlStr
    }

    fun render(template: String?, listContent: String?): String? {
        if (template == null || listContent == null) {
            return null
        }
        return template.replace("{list-content}", listContent)
                .replace("{{googletagservices-js}}", JSCodes.googletagservices)
    }

    companion object {
        // Indicate you need to craw an HTML fragment
        const val HTML_TYPE_FRAGMENT = 1
        // Indicate you need to load a complete web page into webview.
        const val HTML_TYPE_COMPLETE = 2
    }
}