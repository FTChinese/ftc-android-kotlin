package com.ft.ftchinese.repository

import android.util.Log
import com.ft.ftchinese.App
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.store.SessionTokenStore
import com.ft.ftchinese.store.WebAccessTokenStore
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "AndroidAuthToken"

internal object AndroidAuthTokenPersister {

    fun persistFromRaw(raw: String?, source: String) {
        if (raw.isNullOrBlank()) {
            Log.i(TAG, "Skip persist from $source: empty raw response")
            return
        }

        runCatching {
            val root = marshaller.parseToJsonElement(raw).jsonObject
            val sessionToken = root["session"]
                ?.jsonObject
                ?.get("sessionToken")
                ?.jsonPrimitive
                ?.contentOrNull
                ?: root["sessionToken"]
                    ?.jsonPrimitive
                    ?.contentOrNull
            val accessToken = root["accessToken"]
                ?.jsonPrimitive
                ?.contentOrNull

            if (!sessionToken.isNullOrBlank()) {
                SessionTokenStore.getInstance(App.instance).save(sessionToken)
                Log.i(TAG, "Persisted sessionToken from $source: ${maskToken(sessionToken)}")
            } else {
                Log.i(TAG, "No sessionToken in $source response")
            }

            if (!accessToken.isNullOrBlank()) {
                WebAccessTokenStore.getInstance(App.instance).save(accessToken)
                Log.i(TAG, "Persisted accessToken from $source: ${maskToken(accessToken)}")
            } else {
                Log.i(TAG, "No accessToken in $source response")
            }
        }.onFailure {
            Log.w(TAG, "Failed to parse auth tokens from $source: ${it.message}")
        }
    }

    private fun maskToken(token: String): String {
        val trimmed = token.trim()
        if (trimmed.length <= 12) {
            return trimmed
        }
        return "${trimmed.take(8)}...${trimmed.takeLast(4)}"
    }
}
