package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.reader.Credentials
import com.ft.ftchinese.model.reader.ReadingDuration
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.currentFlavor
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.json.JSONException
import org.json.JSONObject
import kotlin.Exception

class ReaderRepo : AnkoLogger {

    fun login(c: Credentials): String? {
        val (_, body) = Fetch()
                .post(NextApi.LOGIN)
                .setClient()
                .noCache()
                .jsonBody(json.toJsonString(c))
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

    /**
     * If unionId is null, it indicates this is a regular registration;
     * If unionId is not null, it indicates a
     * wechat-logged-in user is trying to create a new
     * account and this new account should be bound to this
     * wechat account after creation.
     */
    fun signUp(c: Credentials, unionId: String? = null): String? {
        val fetch = if (unionId != null) {
            Fetch().post(NextApi.WX_SIGNUP)
                    .setUnionId(unionId)
        } else {
            Fetch().post(NextApi.SIGN_UP)
        }


        val (_, body) = fetch
                .setClient()
                .noCache()
                .jsonBody(json.toJsonString(c))
                .responseApi()

        return if (body == null) {
            null
        } else {
            decodeUserId(body)
        }
    }

    fun engaged(dur: ReadingDuration): String? {
        info("Engagement length of ${dur.userId}: ${dur.startUnix} - ${dur.endUnix}")

        return try {
            Fetch().post("${currentFlavor.baseUrl}/engagement.php")
                    .jsonBody(Klaxon().toJsonString(dur))
                    .responseString()

        } catch (e: Exception) {
            info("Error when tracking reading duration $e")
            null
        }
    }

    companion object {
        private var instance: ReaderRepo? = null

        @Synchronized
        fun getInstance(): ReaderRepo {
            if (instance == null) {
                instance = ReaderRepo()
            }

            return instance!!
        }
    }
}
