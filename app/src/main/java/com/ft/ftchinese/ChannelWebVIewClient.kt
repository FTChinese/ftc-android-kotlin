package com.ft.ftchinese

import android.content.Context
import android.net.Uri
import org.jetbrains.anko.info

class ChannelWebVIewClient(private val context: Context?) : AbstractWebViewClient(context) {
    override fun handleInSiteLink(uri: Uri): Boolean {
        val pathSegments = uri.pathSegments


        if (pathSegments.size < 2) {
            /**
             * Handle the pagination link of each channel
             * There's a problem with each channel's pagination: they used relative urls.
             * When loaded in WebView with base url `http://www.ftchinese.com`,
             * the url will become something `http://www.ftchinese.com/china.html?page=2`,
             * which should actually be `http://www.ftchiese.com/channel/china.html?page=2`
             */
            val queryPage = uri.getQueryParameter("page")
            if (queryPage != null) {
                val page = ListPage(
                        title = currentPage?.title ?: "",
                        name = "channel_p${queryPage}_${uri.lastPathSegment}",
                        listUrl = buildUrl(uri, "/channel/${uri.path}")
                )

                /**
                 * Start a new page of article list.
                 */
                ChannelActivity.start(context, page)
                return true
            }

            /**
             * Assume this is a content page and load the url directly.
             */
            ContentActivity.start(context, buildUrl(uri))
            return true
        }


        when (pathSegments[0]) {

            "channel" -> {
                return handleChannel(uri)
            }

            "m" -> {
                return handleMarketing(uri)
            }

        // If the path looks like `/story/001078593`
            "story" -> {
                val channelItem = ChannelItem(id = pathSegments[1], type = pathSegments[0], headline = "", shortlead = "")
                ContentActivity.start(activity, channelItem)
            }

        //
        /**
         * If the path looks like `/tag/中美贸易战`,
         * start a new page listing articles
         */
            "tag" -> {
                val page = ListPage(
                        title = pathSegments[1],
                        name = "${pathSegments[0]}_${pathSegments[1]}",
                        listUrl = buildUrl(uri)
                )

                ChannelActivity.start(activity, page)
            }

            else -> {
                info("Open a content link directly. Original url is: $uri. API url is ${buildUrl(uri)}")
                ContentActivity.start(context, buildUrl(uri))
            }
        }

        return true
    }

    private fun handleChannel(uri: Uri): Boolean {

        val lastPathSegment = uri.lastPathSegment

        if (lastPathSegment == null) {
            val page = ListPage(
                    title = "",
                    name = "",
                    listUrl = buildUrl(uri)
            )

            ChannelActivity.start(activity, page)
            return true
        }

        when (lastPathSegment) {
        /**
         * If the path is `/channel/english.html`, navigate to the second bottom nav item.
         */
            "english.html" -> {
                navigateListener.selectBottomNavItem(R.id.nav_english)
            }


        /**
         * If the path is `/channel/mba.html`, navigate to the third bottom nav item
         */
            "mba.html" -> {
                navigateListener.selectBottomNavItem(R.id.nav_ftacademy)
            }


        /**
         * If the path is `/channel/weekly.html`
         */
            "weekly.html" -> {

                val tabIndex = ListPage.newsPages.indexOfFirst { it.name == "news_top_stories" }

                navigateListener.selectTabLayoutTab(tabIndex)
            }

            "markets.html" -> {
                val tabIndex = ListPage.newsPages.indexOfFirst { it.name == "news_markets" }

                navigateListener.selectTabLayoutTab(tabIndex)
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

                val page = ListPage(
                        title = pathToTitle[lastPathSegment] ?: "",
                        name = name,
                        listUrl = buildUrl(uri)
                )
                ChannelActivity.start(activity, page)
            }
        }

        return true
    }

    private fun handleMarketing(uri: Uri): Boolean {
        if (uri.pathSegments[1] == "marketing") {
            when (uri.lastPathSegment) {

            /**
             * If the path is `/m/marketing/intelligence.html`,
             * navigate to the tab titled FT研究院
             */
                "intelligence.html" -> {
                    val tabIndex = ListPage.newsPages.indexOfFirst { it.name == "news_fta" }

                    navigateListener.selectTabLayoutTab(tabIndex)
                }

            /**
             * If the path looks like `/m/marketing/businesscase.html`
             */
                else -> {
                    val name = uri.lastPathSegment ?: ""

                    val page = ListPage(
                            title = pathToTitle[name] ?: "",
                            name = "marketing_$name",
                            listUrl = buildUrl(uri)
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
        val page = ListPage(
                title = "",
                name = "",
                listUrl = buildUrl(uri)
        )
        ChannelActivity.start(activity, page)

        return true
    }

    private fun buildUrl(uri: Uri, path: String? = null): String {
        val builder =  uri.buildUpon()
                .scheme("https")
                .authority("api003.ftmailbox.com")
                .appendQueryParameter("bodyonly", "yes")
                .appendQueryParameter("webview", "ftcapp")

        if (path != null) {
            builder.path(path)
        }

        return builder.build().toString()
    }

}