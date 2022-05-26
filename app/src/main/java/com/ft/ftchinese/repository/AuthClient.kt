package com.ft.ftchinese.repository

import android.util.Log
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AuthClient {

    fun emailExists(email: String): Boolean {
        try {
            val resp = Fetch()
                .get(Endpoint.emailExists)
                .addQuery("v", email)
                .noCache()
                .setApiKey()
                .endText()

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
            .setApiKey()
            .setClient()
            .sendJson(c)
            .endJson<Account>()
            .body
    }

    fun emailSignUp(c: Credentials): Account? {
        return Fetch()
            .post(Endpoint.emailSignUp)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(c)
            .endJson<Account>()
            .body
    }

    fun requestSMSCode(params: SMSCodeParams): Boolean {
        val resp = Fetch()
            .put(Endpoint.mobileVerificationCode)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(params)
            .endOrThrow()

        return resp.code == 204
    }

    suspend fun asyncSMSAuthCode(params: SMSCodeParams): FetchResult<Boolean> {
        return try {
            val ok = withContext(Dispatchers.IO) {
                requestSMSCode(params)
            }

            if (ok) {
                FetchResult.Success(true)
            } else {
                FetchResult.loadingFailed
            }
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }



    fun verifySMSCode(params: MobileAuthParams): UserFound? {
        return Fetch()
            .post(Endpoint.mobileVerificationCode)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(params)
            .endJson<UserFound>()
            .body
    }

    fun mobileLinkExistingEmail(params: MobileLinkParams): Account? {
        return Fetch()
            .post(Endpoint.mobileInitialLink)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(params)
            .endJson<Account>()
            .body
    }

    /**
     * Create a new account with email derived from phone number.
     */
    fun mobileSignUp(params: MobileSignUpParams): Account? {
        return Fetch()
            .post(Endpoint.mobileSignUp)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(params)
            .endJson<Account>()
            .body
    }

    fun passwordResetLetter(params: PasswordResetLetterParams): Boolean {
        val resp = Fetch()
            .post(Endpoint.passwordResetLetter)
            .setTimeout(30)
            .setClient()
            .noCache()
            .setApiKey()
            .sendJson(params)
            .endText()

        return resp.code == 204
    }

    fun verifyPwResetCode(v: PasswordResetVerifier): PwResetBearer? {
        return Fetch()
            .get("${Endpoint.passwordResetCodes}?email=${v.email}&code=${v.code}")
            .noCache()
            .setApiKey()
            .endJson<PwResetBearer>()
            .body
    }

    fun resetPassword(params: PasswordResetParams): Boolean {
        val resp = Fetch()
            .post(Endpoint.passwordReset)
            .setClient()
            .noCache()
            .setApiKey()
            .sendJson(params)
            .endText()

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
            .setApiKey()
            .setTimeout(30)
            .sendJson(params)
            .endJson<WxSession>()
            .body
    }

    suspend fun asyncWxLogin(params: WxAuthParams): FetchResult<WxSession> {
        try {
            val sess = withContext(Dispatchers.IO) {
                wxLogin(params)
            } ?: return FetchResult.loadingFailed

            // Fetched wx session data and send it to
            // UI thread for saving, and then continues
            // to fetch account data.

            return FetchResult.Success(sess)
        } catch (e: Exception) {
            // If the error is ClientError,
            // Possible 422 error key: code_missing_field, code_invalid.
            // We cannot make sure the exact meaning of each error, just
            // show user API's error message.
            return FetchResult.fromException(e)
        }
    }

    fun engaged(dur: ReadingDuration): String? {
        Log.i("AuthClient", "Engagement length of ${dur.userId}: ${dur.startUnix} - ${dur.endUnix}")

        return Fetch().post("${dur.refer}/engagement.php")
            .sendJson(dur)
            .endText()
            .body
    }
}
