package com.ft.ftchinese

import android.app.Activity
import android.net.Uri
import com.ft.ftchinese.models.PagerTab
import com.ft.ftchinese.models.pathToTitle

/**
 * A WebViewClient used by a ChannelFragment
 */
class ChannelWebViewClient(
        activity: Activity?,
        private val currentPage: PagerTab?
) : BaseWebViewClient(activity) {

    /**
     * Callback used by ChannelWebViewClient.
     * When certain links in web view is clicked, the event is passed to parent activity to open a bottom navigation item or a tab.
     */
    private var mListener: OnInAppNavigate? = null
    /**
     * Jump to another bottom navigation item or another tab in hte same bottom navigation item when certain links are clicked.
     */
    interface OnInAppNavigate {
        // Jump to a new bottom navigation item
        fun selectBottomNavItem(itemId: Int)

        // Go to another tab
        fun selectTabLayoutTab(tabIndex: Int)
    }

    fun setOnInAppNavigateListener(listener: OnInAppNavigate?) {
        mListener = listener
    }

    override fun openChannelPagination(uri: Uri): Boolean {
//        val queryPage = uri.getQueryParameter("page") ?: return false

        val page = PagerTab(
                title = currentPage?.title ?: "",
                // Pagination should not be cached since it always dynamic
                name = "",
                fragmentUrl = buildUrl(uri, "/channel/${uri.path}"))

        /**
         * Start a new page of article list.
         */
        ChannelActivity.start(activity, page)
        return true

    }

    /**
     * Handle urls whose path start with `/channel/...`
     */
    override fun openChannelLink(uri: Uri): Boolean {

        val lastPathSegment = uri.lastPathSegment

        /**
         * Just a precaution to handle any unexpected url.
         */
        if (lastPathSegment == null) {
            val page = PagerTab(
                    title = "",
                    name = "",
                    fragmentUrl = buildUrl(uri)
            )

            ChannelActivity.start(activity, page)
            return true
        }

        when (lastPathSegment) {
        /**
         * If the path is `/channel/english.html`, navigate to the second bottom nav item.
         */
            "english.html" -> {
                mListener?.selectBottomNavItem(R.id.nav_english)
            }


        /**
         * If the path is `/channel/mba.html`, navigate to the third bottom nav item
         */
            "mba.html" -> {
                mListener?.selectBottomNavItem(R.id.nav_ftacademy)
            }


        /**
         * If the path is `/channel/weekly.html`
         */
            "weekly.html" -> {

                val tabIndex = PagerTab.newsPages.indexOfFirst { it.name == "news_top_stories" }

                mListener?.selectTabLayoutTab(tabIndex)
            }

            "markets.html" -> {
                val tabIndex = PagerTab.newsPages.indexOfFirst { it.name == "news_markets" }

                mListener?.selectTabLayoutTab(tabIndex)
            }

        /**
         * Handle paths like:
         * `/channel/editorchoice-issue.html?issue=EditorChoice-xxx`,
         * `/channel/chinabusinesswatch.html`
         * `/channel/viewtop.html`
         * `/channel/teawithft.html`
         * `/channel/markets.html`
         * `/channel/money.html`
         */
            else -> {
                val issue = uri.getQueryParameter("issue")
                val name = issue ?: "channel_$lastPathSegment"

                val page = PagerTab(
                        title = pathToTitle[lastPathSegment] ?: "",
                        name = name,
                        fragmentUrl = buildUrl(uri))

                ChannelActivity.start(activity, page)
            }
        }

        return true
    }

    override fun openMarketingLink(uri: Uri): Boolean {
        if (uri.pathSegments[1] == "marketing") {
            when (uri.lastPathSegment) {

            /**
             * If the path is `/m/marketing/intelligence.html`,
             * navigate to the tab titled FT研究院
             */
                "intelligence.html" -> {
                    val tabIndex = PagerTab.newsPages.indexOfFirst { it.name == "news_fta" }

                    mListener?.selectTabLayoutTab(tabIndex)
                }

            /**
             * If the path looks like `/m/marketing/businesscase.html`
             */
                else -> {
                    val name = uri.lastPathSegment ?: ""

                    val page = PagerTab(
                            title = pathToTitle[name] ?: "",
                            name = "marketing_$name",
                            fragmentUrl = buildUrl(uri)
                    )
                    ChannelActivity.start(activity, page)
                }
            }

            return true
        }


        /**
         * There URLs looks like: `/m/corp/preview.html?pageid=we2016&isad=1`.
         * Don't bother with them
         */
        val page = PagerTab(
                title = "",
                name = "",
                fragmentUrl = buildUrl(uri)
        )
        ChannelActivity.start(activity, page)

        return true
    }

}