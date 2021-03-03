package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.fetch.json
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object ReaderRepo : AnkoLogger {

    fun emailExists(email: String): Boolean {

        val (resp, _) = Fetch()
                .get(NextApi.EMAIL_EXISTS)
                .query("k", "email")
                .query("v", email)
                .noCache()
                .endJsonText()

        return resp.code == 204
    }

    fun login(c: Credentials): Account? {
        val (_, body) = Fetch()
                .post(NextApi.LOGIN)
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

    fun passwordResetLetter(email: String): Boolean {
        val (response, _)= Fetch()
            .post(NextApi.PASSWORD_RESET_LETTER)
            .setTimeout(30)
            .noCache()
            .sendJson(json.toJsonString(mapOf("email" to email)))
            .endJsonText()

        return response.code == 204
    }

    fun verifyPwResetCode(v: PwResetVerifier): PwResetBearer? {
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

    fun resetPassword(v: PasswordResetter): Boolean {
        val (resp, _) = Fetch()
            .post(NextApi.PASSWORD_RESET)
            .setClient()
            .noCache()
            .sendJson(json.toJsonString(v))
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
