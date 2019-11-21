package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Credentials
import com.ft.ftchinese.model.reader.ReadingDuration
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.util.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import kotlin.Exception

class ReaderRepo : AnkoLogger {

    fun emailExists(email: String): Boolean {

        val (resp, _) = Fetch()
                .get(NextApi.EMAIL_EXISTS)
                .query("k", "email")
                .query("v", email)
                .noCache()
                .responseApi()

        return resp.code == 204
    }

    fun login(c: Credentials): Account? {
        val (_, body) = Fetch()
                .post(NextApi.LOGIN)
                .setClient()
                .noCache()
                .jsonBody(json.toJsonString(c))
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun signUp(c: Credentials): Account? {

        val (_, body) = Fetch()
                .post(NextApi.SIGN_UP)
                .setClient()
                .noCache()
                .jsonBody(json.toJsonString(c))
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun passwordResetLetter(email: String): Boolean {
        val (response, _)= Fetch()
                .post(NextApi.PASSWORD_RESET)
                .setTimeout(30)
                .noCache()
                .jsonBody(json.toJsonString(mapOf("email" to email)))
                .responseApi()

        return response.code == 204
    }

    /**
     * Wecaht login does not return an [Account] instance.
     * It returns a [WxSession] which represents the access
     * token acquired from Wechat API.
     * Then you can use [WxSession] to retrieve Wechat
     * info or refresh account data.
     */
    fun wxLogin(code: String): WxSession? {
        val (_, body) = Fetch().post(SubscribeApi.WX_LOGIN)
                .setClient()
                .setAppId()
                .noCache()
                .jsonBody(json.toJsonString(mapOf(
                        "code" to code
                )))
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<WxSession>(body)
        }
    }

    /**
     * Fetch user account data after [wxLogin] succeeded.
     * Account retrieved from here always has loginMethod set to `wechat`.
     * Only used for initial login.
     * DO NOT use this to refresh account data since WxSession only exists
     * if user logged in via wechat OAuth.
     * If user logged in with email + password (and the the email is bound to this wechat),
     * WxSession never actually exists.
     */
    fun loadWxAccount(unionId: String): Account? {
        val (_, body) = Fetch()
                .get(NextApi.WX_ACCOUNT)
                .setUnionId(unionId)
                .noCache()
                .responseApi()

        return if (body == null) {
            return null
        } else {
            json.parse<Account>(body)
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
}
