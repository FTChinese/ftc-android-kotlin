package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.ServerError
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.request.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object AuthClient : AnkoLogger {

    fun emailExists(email: String): Boolean {
        try {
            val (resp, _) = Fetch()
                .get(Endpoint.emailExists)
                .query("v", email)
                .noCache()
                .endJsonText()

            // Code below 400
            if (resp.code != 204) {
                throw ServerError(
                    message = "Unexpected status code ${resp.code}",
                    statusCode = resp.code
                )
            }
            return true
        } catch (e: ServerError) {
            if (e.statusCode == 404) {
                return false
            }

            throw e
        }
    }

    fun login(c: Credentials): Account? {
        val (_, body) = Fetch()
            .post(Endpoint.emailLogin)
            .noCache()
            .setClient()
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
            .post(Endpoint.emailSignUp)
            .noCache()
            .setClient()
            .sendJson(c.toJsonString())
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun requestSMSCode(params: SMSCodeParams): Boolean {
        val (resp, _) = Fetch()
            .put(Endpoint.mobileVerificationCode)
            .noCache()
            .setClient()
            .sendJson(params.toJsonString())
            .endJsonText()

        return resp.code == 204
    }

    fun verifySMSCode(params: MobileAuthParams): UserFound? {
        val (_, body) = Fetch()
            .post(Endpoint.mobileVerificationCode)
            .noCache()
            .setClient()
            .sendJson(params.toJsonString())
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<UserFound>(body)
        }
    }

    fun mobileLinkEmail(params: MobileLinkParams): Account? {
        val (_, body) = Fetch()
            .post(Endpoint.mobileInitialLink)
            .noCache()
            .setClient()
            .sendJson(params.toJsonString())
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun mobileSignUp(params: MobileLinkParams): Account? {
        val (_, body) = Fetch()
            .post(Endpoint.mobileSignUp)
            .noCache()
            .setClient()
            .sendJson(params.toJsonString())
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }

    fun passwordResetLetter(params: PasswordResetLetterParams): Boolean {
        val (response, _)= Fetch()
            .post(Endpoint.passwordResetLetter)
            .setTimeout(30)
            .setClient()
            .noCache()
            .sendJson(json.toJsonString(params))
            .endJsonText()

        return response.code == 204
    }

    fun verifyPwResetCode(v: PasswordResetVerifier): PwResetBearer? {
        val (_, body) = Fetch()
            .get("${Endpoint.passwordResetCodes}?email=${v.email}&code=${v.code}")
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
            .post(Endpoint.passwordReset)
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
    fun wxLogin(params: WxAuthParams): WxSession? {
        val (_, body) = Fetch().post(Endpoint.wxLogin)
            .setClient()
            .setAppId()
            .noCache()
            .setTimeout(30)
            .sendJson(params.toJsonString())
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
