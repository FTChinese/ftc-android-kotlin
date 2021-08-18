package com.ft.ftchinese.repository

import android.util.Log
import com.ft.ftchinese.model.content.Story
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.JSONResult
import com.ft.ftchinese.model.fetch.json

object ArticleClient {
    private const val TAG = "ArticleClient"

    /**
     * Load JSON documents from server. The raw json string
     * is returned together with the parsed doc so that
     * we don't need to re-serialize the parsed JSON.
     */
    fun fetchStory(teaser: Teaser, baseUrl: String): JSONResult<Story>? {
        val url = teaser.articleUrl(baseUrl)

        Log.i(TAG, "Loading article data from $url")

        val body = Fetch()
            .get(url)
            .endPlainText()

        if (body.isNullOrBlank()) {
            return null
        }

        val s = json.parse<Story>(body)
        return if (s == null) {
            null
        } else {
            s.teaser = teaser
            JSONResult(s, body)
        }
    }

    fun crawlHtml(url: String): String? {
        return Fetch()
            .get(url)
            .endPlainText()
    }
}
