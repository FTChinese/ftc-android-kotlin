package com.ft.ftchinese.repository

import com.ft.ftchinese.model.apicontent.BilingualStory
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.util.json

object ContentAPIRepo {
    fun loadStory(teaser: Teaser): String? {
        val url = when (teaser.type) {
            ArticleType.Story,
            ArticleType.Premium -> "${ContentApi.STORY}/${teaser.id}/ce"
            ArticleType.Interactive -> "${ContentApi.INTERACTIVE}/${teaser.id}"
            else -> throw Exception("invalid url")
        }

        val (_, body) = Fetch()
                .get(url)
                .responseApi()

        return body
    }
}
