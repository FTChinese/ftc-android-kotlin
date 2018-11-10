package com.ft.ftchinese.models

import org.jetbrains.anko.AnkoLogger

/**
 * PagerTab contains the data used by a page in ViewPager
 */
data class PagerTab (
        val title: String, // A Tab's title
        // name is used to cache files.
        // If empty, do not cache it, nor should you try to
        // find cache.
        val name: String,  // Cache filename used by this tab
        val contentUrl: String, // This is used to fetch html fragment containing a list of articles.
        val htmlType: Int // Flag used to tell whether the url should be loaded directly
) : AnkoLogger {

    val fileName: String
        get() = if (name.isBlank()) "" else "$name.html"


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