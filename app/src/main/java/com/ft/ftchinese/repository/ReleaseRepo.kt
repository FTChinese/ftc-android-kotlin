package com.ft.ftchinese.repository

import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger

object ReleaseRepo : AnkoLogger {
    fun retrieveRelease(versionName: String): String? {
        val (_, body) = Fetch()
                .setAppId()
                .get("${NextApi.releaseOf}/${normalizeVersionName(versionName)}")
                .responseApi()

        return body
    }

    fun latestRelease(): AppRelease? {
        val (_, body) = Fetch()
                .setAppId()
                .get(NextApi.latestRelease)
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse(body)
        }
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
