package com.ft.ftchinese.repository

import com.ft.ftchinese.model.content.Story
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.JSONResult
import com.ft.ftchinese.model.fetch.json
import org.jetbrains.anko.AnkoLogger

object ArticleClient : AnkoLogger {
    fun fetchStory(url: String): JSONResult<Story>? {
        val body = Fetch()
            .get(url).
            endPlainText()

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
}
