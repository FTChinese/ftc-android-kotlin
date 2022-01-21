package com.ft.ftchinese.repository

import android.util.Log
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.request.*

object AuthClient {

    fun emailExists(email: String): Boolean {
        try {
            val resp = Fetch()
                .get(Endpoint.emailExists)
                .addQuery("v", email)
                .noCache()
                .endApiText()

            // Code below 400
            if (resp.code != 204) {
                throw APIError(
                    message = "Unexpected status code ${resp.code}",
                    statusCode = resp.code
                )
            }
            return true
        } catch (e: APIError) {
            if (e.statusCode == 404) {
                return false
            }

            throw e
        }
    }

    fun emailLogin(c: Credentials): Account? {
        return Fetch()
            .post(Endpoint.emailLogin)
            .noCache()
            .setClient()
            .sendJson(c.toJsonString())
            .endApiJson<Account>()
            .body
    }

    fun emailSignUp(c: Credentials): Account? {
        return Fetch()
            .post(Endpoint.emailSignUp)
            .noCache()
            .setClient()
            .sendJson(c.toJsonString())
            .endApiJson<Account>()
            .body
    }

    fun requestSMSCode(params: SMSCodeParams): Boolean {
        val resp = Fetch()
            .put(Endpoint.mobileVerificationCode)
            .noCache()
            .setClient()
            .sendJson(params.toJsonString())
            .endApiText()

        return resp.code == 204
    }

    fun verifySMSCode(params: MobileAuthParams): UserFound? {
        return Fetch()
            .post(Endpoint.mobileVerificationCode)
            .noCache()
            .setClient()
            .sendJson(params.toJsonString())
            .endApiJson<UserFound>()
            .body
    }

    fun mobileLinkExistingEmail(params: MobileLinkParams): Account? {
        return Fetch()
            .post(Endpoint.mobileInitialLink)
            .noCache()
            .setClient()
            .sendJson(params.toJsonString())
            .endApiJson<Account>()
            .body
    }

    /**
     * Create a new account with email derived from phone number.
     */
    fun mobileSignUp(params: MobileSignUpParams): Account? {
        return Fetch()
            .post(Endpoint.mobileSignUp)
            .noCache()
            .setClient()
            .sendJson(params.toJsonString())
            .endApiJson<Account>()
            .body
    }

    fun passwordResetLetter(params: PasswordResetLetterParams): Boolean {
        val resp = Fetch()
            .post(Endpoint.passwordResetLetter)
            .setTimeout(30)
            .setClient()
            .noCache()
            .sendJson(json.toJsonString(params))
            .endApiText()

        return resp.code == 204
    }

    fun verifyPwResetCode(v: PasswordResetVerifier): PwResetBearer? {
        return Fetch()
            .get("${Endpoint.passwordResetCodes}?email=${v.email}&code=${v.code}")
            .noCache()
            .endApiJson<PwResetBearer>()
            .body
    }

    fun resetPassword(params: PasswordResetParams): Boolean {
        val resp = Fetch()
            .post(Endpoint.passwordReset)
            .setClient()
            .noCache()
            .sendJson(json.toJsonString(params))
            .endApiText()

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
        return Fetch().post(Endpoint.wxLogin)
            .setClient()
            .setAppId()
            .noCache()
            .setTimeout(30)
            .sendJson(params.toJsonString())
            .endApiJson<WxSession>()
            .body
    }

    fun engaged(dur: ReadingDuration): String? {
        Log.i("AuthClient", "Engagement length of ${dur.userId}: ${dur.startUnix} - ${dur.endUnix}")

        return Fetch().post("${dur.refer}/engagement.php")
            .sendJson(Klaxon().toJsonString(dur))
            .endText()
            .body
    }
}
