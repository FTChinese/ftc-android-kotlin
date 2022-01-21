package com.ft.ftchinese.repository

import android.util.Log
import com.ft.ftchinese.model.content.Story
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.HttpResp

object ArticleClient {
    private const val TAG = "ArticleClient"

    /**
     * Load JSON documents from server. The raw json string
     * is returned together with the parsed doc so that
     * we don't need to re-serialize the parsed JSON.
     */
    fun fetchStory(teaser: Teaser, baseUrl: String): HttpResp<Story> {
        val url = teaser.articleUrl(baseUrl)

        Log.i(TAG, "Loading article data from $url")

        val resp = Fetch()
            .get(url)
            .endJson<Story>()


        resp.body?.teaser = teaser

        return resp
    }

    fun crawlHtml(url: String): String? {
        return Fetch()
            .get(url)
            .endText()
            .body
    }
}
