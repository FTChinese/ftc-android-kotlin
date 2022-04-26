package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.HttpResp

object ArticleClient {

    fun crawlFile(url: String): HttpResp<String> {
        return Fetch()
            .get(url)
            .endText()
    }
}
