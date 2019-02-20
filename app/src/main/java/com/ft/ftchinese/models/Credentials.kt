package com.ft.ftchinese.models

import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.json.JSONException
import org.json.JSONObject

data class Credentials(
        val email: String,
        val password: String
) : AnkoLogger {

    fun login(): String? {
        val (_, body) = Fetch()
                .post(NextApi.LOGIN)
                .setClient()
                .noCache()
                .jsonBody(json.toJsonString(this))
                .responseApi()

        return if (body == null) {
            null
        } else {
            decodeUserId(body)
        }
    }

    private fun decodeUserId(json: String): String? {
        return try {
            JSONObject(json).getString("id")
        } catch (e: JSONException) {
            info(e)
            null
        }
    }

    fun signUp(): String? {
        val (_, body) = Fetch()
                .post(NextApi.SIGN_UP)
                .setClient()
                .noCache()
                .jsonBody(json.toJsonString(this))
                .responseApi()

        return if (body == null) {
            null
        } else {
            decodeUserId(body)
        }
    }
}