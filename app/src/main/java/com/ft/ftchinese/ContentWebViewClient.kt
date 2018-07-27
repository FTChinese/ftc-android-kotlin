package com.ft.ftchinese

import android.content.Context
import android.net.Uri

class ContentWebViewClient(private val context: Context?) : AbstractWebViewClient(context) {

    override fun handleInSiteLink(uri: Uri): Boolean {
        val pathSegments = uri.pathSegments

        if (pathSegments.size >= 2 && pathSegments[0] == "story") {
            val channelItem = ChannelItem(id = pathSegments[1], type = pathSegments[0], headline = "", shortlead = "")

            StoryActivity.start(context, channelItem)

            return true
        }

        WebContentActivity.start(context, buildUrl(uri))

        return true
    }

    private fun buildUrl(uri: Uri): String {
        return uri.buildUpon()
                .scheme("https")
                .authority("api003.ftmailbox.com")
                .appendQueryParameter("bodyonly", "yes")
                .appendQueryParameter("webview", "ftcapp")
                .build()
                .toString()
    }
}