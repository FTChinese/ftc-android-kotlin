package com.ft.ftchinese.repository

import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.HttpResp

object ReleaseRepo {

    fun getRelease(versionName: String): HttpResp<AppRelease> {
        return Fetch()
            .setAppId()
            .setApiKey()
            .get("${Endpoint.releaseOf}/${normalizeVersionName(versionName)}")
            .endJson(withRaw = true)
    }

    fun getLatest(): HttpResp<AppRelease> {
        return Fetch()
            .setAppId()
            .setApiKey()
            .get(Endpoint.latestRelease)
            .endJson(withRaw = true)
    }

    private fun normalizeVersionName(versionName: String): String {
        val parts = versionName.split("-")
        if (parts.isEmpty()) {
            return versionName
        }

        val name = parts[0]
        if (name.startsWith("v")) {
            return name
        }

        return "v$name"
    }
}
