package com.ft.ftchinese

import android.app.Activity
import android.net.Uri
import android.webkit.WebViewClient
import com.ft.ftchinese.models.Navigation
import com.ft.ftchinese.models.PagerTab
import com.ft.ftchinese.models.pathToTitle
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * A WebViewClient used by a ChannelFragment。
 * Handles click on a url.
 */
class ChannelWebViewClient(
        private val activity: Activity?
) : WebViewClient(), AnkoLogger {

    /**
     * Callback used by ChannelWebViewClient.
     * When certain links in web view is clicked, the event is passed to parent activity to open a bottom navigation item or a tab.
     */
    private var mListener: OnPaginateListener? = null
    /**
     * Jump to another bottom navigation item or another tab in hte same bottom navigation item when certain links are clicked.
     */
    interface OnPaginateListener {
        // Jump to a new bottom navigation item
        fun selectBottomNavItem(itemId: Int)

        // Go to another tab
        fun selectTabLayoutTab(tabIndex: Int)
    }

    fun setOnPaginateListener(listener: OnPaginateListener?) {
        mListener = listener
    }

    /**
     * Handle urls whose path start with `/channel/...`
     */
//    override fun openChannelLink(uri: Uri): Boolean {
//
//        val lastPathSegment = uri.lastPathSegment
//
//        /**
//         * Just a precaution to handle any unexpected url.
//         */
//        if (lastPathSegment == null) {
//            val page = PagerTab(
//                    title = "",
//                    name = "",
//                    contentUrl = buildUrl(uri),
//                    htmlType = PagerTab.HTML_TYPE_FRAGMENT
//            )
//
//            ChannelActivity.start(activity, page)
//            return true
//        }
//
//        when (lastPathSegment) {
//        /**
//         * If the path is `/channel/english.html`, navigate to the second bottom nav item.
//         */
//            "english.html" -> {
//                mListener?.selectBottomNavItem(R.id.nav_english)
//            }
//
//
//        /**
//         * If the path is `/channel/mba.html`, navigate to the third bottom nav item
//         */
//            "mba.html" -> {
//                mListener?.selectBottomNavItem(R.id.nav_ftacademy)
//            }
//
//
//        /**
//         * If the path is `/channel/weekly.html`
//         */
//            "weekly.html" -> {
//
//                val tabIndex = Navigation.newsPages.indexOfFirst { it.name == "news_top_stories" }
//
//                mListener?.selectTabLayoutTab(tabIndex)
//            }
//
//            "markets.html" -> {
//                val tabIndex = Navigation.newsPages.indexOfFirst { it.name == "news_markets" }
//
//                mListener?.selectTabLayoutTab(tabIndex)
//            }
//
//        /**
//         * Handle paths like:
//         * `/channel/editorchoice-issue.html?issue=EditorChoice-xxx`,
//         * `/channel/chinabusinesswatch.html`
//         * `/channel/viewtop.html`
//         * `/channel/teawithft.html`
//         * `/channel/markets.html`
//         * `/channel/money.html`
//         */
//            else -> {
//                info("Open a channel link")
//                // First try to find out if this a editor's choice
//                val issue = uri.getQueryParameter("issue")
//                // Build filename to be used for cache.
//                val name = issue ?: "channel_$lastPathSegment"
//
//                val listPage = PagerTab(
//                        // Translate path to human readable text
//                        title = pathToTitle[lastPathSegment] ?: "",
//                        name = name,
//                        contentUrl = buildUrl(uri),
//                        htmlType = PagerTab.HTML_TYPE_FRAGMENT
//                )
//
//                ChannelActivity.start(activity, listPage)
//            }
//        }
//
//        return true
//    }
}