package com.ft.ftchinese.repository

import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.Teaser
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object ContentAPIRepo : AnkoLogger {
    fun loadStory(teaser: Teaser): String? {
        val url = when (teaser.type) {
            ArticleType.Story,
            ArticleType.Premium -> "${ContentApi.STORY}/${teaser.id}/ce"
            ArticleType.Interactive -> "${ContentApi.INTERACTIVE}/${teaser.id}"
            else -> throw Exception("invalid url")
        }

        info("Loading article from $url for $teaser")

        val (_, body) = Fetch()
                .get(url)
                .responseApi()

        return body
    }
}
