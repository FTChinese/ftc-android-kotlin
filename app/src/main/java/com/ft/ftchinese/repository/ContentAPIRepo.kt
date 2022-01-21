package com.ft.ftchinese.repository

import android.util.Log
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.fetch.Fetch

object ContentAPIRepo {
    const val TAG = "ContentAPIRepo"

    fun loadStory(teaser: Teaser): String? {
        val url = when (teaser.type) {
            ArticleType.Story,
            ArticleType.Premium -> "${ContentApi.STORY}/${teaser.id}/ce"
            ArticleType.Interactive -> "${ContentApi.INTERACTIVE}/${teaser.id}"
            else -> throw Exception("invalid url")
        }

        Log.i(TAG, "Loading article from $url for $teaser")

        return Fetch()
            .get(url)
            .endApiText()
            .body
    }
}
