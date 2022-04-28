package com.ft.ftchinese.repository

import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.HttpResp

object ReleaseRepo {

    fun getLatest(): HttpResp<AppRelease> {
        return Fetch()
            .setAppId()
            .setApiKey()
            .get(Endpoint.latestRelease)
            .endJson(withRaw = true)
    }

}
