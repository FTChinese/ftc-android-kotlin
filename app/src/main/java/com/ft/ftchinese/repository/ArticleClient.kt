package com.ft.ftchinese.repository

import com.ft.ftchinese.model.content.Story
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.JSONResult
import com.ft.ftchinese.model.fetch.json
import org.jetbrains.anko.AnkoLogger

object ArticleClient : AnkoLogger {
    /**
     * Load JSON documents from server. The raw json string
     * is returned together with the parsed doc so that
     * we don't need to re-serialize the parsed JSON.
     */
    fun fetchStory(teaser: Teaser, baseUrl: String): JSONResult<Story>? {
        val body = Fetch()
            .get("${baseUrl}${teaser.apiPathSegment()}")
            .endPlainText()

        if (body.isNullOrBlank()) {
            return null
        }

        val s = json.parse<Story>(body)
        return if (s == null) {
            null
        } else {
            JSONResult(s, body)
        }
    }

    fun crawlHtml(url: String): String? {
        return Fetch()
            .get(url)
            .endPlainText()
    }
}
