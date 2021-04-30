package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.request.PasswordResetLetterParams
import com.ft.ftchinese.model.request.PasswordResetParams
import com.ft.ftchinese.model.request.PasswordResetVerifier
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object AuthClient : AnkoLogger {

    fun emailExists(email: String): Boolean {

        val (resp, _) = Fetch()
            .get(Endpoint.emailExists)
            .query("v", email)
            .noCache()
            .endJsonText()

        return resp.code == 204
    }

    fun login(c: Credentials): Account? {
        val (_, body) = Fetch()
            .post(Endpoint.emailLogin)
            .noCache()
            .sendJson(json.toJsonString(c))
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun signUp(c: Credentials): Account? {

        val (_, body) = Fetch()
                .post(NextApi.SIGN_UP)
                .noCache()
                .sendJson(json.toJsonString(c))
                .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun requestSMSCode(): Boolean {
        return true
    }

    fun passwordResetLetter(params: PasswordResetLetterParams): Boolean {
        val (response, _)= Fetch()
            .post(NextApi.PASSWORD_RESET_LETTER)
            .setTimeout(30)
            .noCache()
            .sendJson(json.toJsonString(params))
            .endJsonText()

        return response.code == 204
    }

    fun verifyPwResetCode(v: PasswordResetVerifier): PwResetBearer? {
        val (_, body) = Fetch()
            .get("${NextApi.VERIFY_PW_RESET}?email=${v.email}&code=${v.code}")
            .noCache()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<PwResetBearer>(body)
        }
    }

    fun resetPassword(params: PasswordResetParams): Boolean {
        val (resp, _) = Fetch()
            .post(NextApi.PASSWORD_RESET)
            .setClient()
            .noCache()
            .sendJson(json.toJsonString(params))
            .endJsonText()

        return resp.code == 204
    }

    /**
     * Wecaht login does not return an [Account] instance.
     * It returns a [WxSession] which represents the access
     * token acquired from Wechat API.
     * Then you can use [WxSession] to retrieve Wechat
     * info or refresh account data.
     */
    fun wxLogin(code: String): WxSession? {
        val (_, body) = Fetch().post(SubsApi.WX_LOGIN)
                .setClient()
                .setAppId()
                .noCache()
                .setTimeout(30)
                .sendJson(json.toJsonString(mapOf(
                        "code" to code
                )))
                .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<WxSession>(body)
        }
    }

    fun engaged(dur: ReadingDuration): String? {
        info("Engagement length of ${dur.userId}: ${dur.startUnix} - ${dur.endUnix}")

        return Fetch().post("${dur.refer}/engagement.php")
            .sendJson(Klaxon().toJsonString(dur))
            .endPlainText()
    }
}
