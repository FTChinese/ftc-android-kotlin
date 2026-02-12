package com.ft.ftchinese.repository

import android.util.Log
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AuthClient {

    fun emailExists(email: String): Boolean {
        val resp = Fetch()
            .get(Endpoint.emailExists)
            .addQuery("v", email)
            .noCache()
            .setApiKey()
            .endOrThrow()

        return resp.code == 204
    }

    suspend fun asyncEmailExists(email: String): FetchResult<Boolean> {
        try {
            val ok = withContext(Dispatchers.IO) {
                emailExists(email)
            }

            return if (ok) {
                FetchResult.Success(true)
            } else {
                FetchResult.loadingFailed
            }
        } catch (e: APIError) {

            return if (e.statusCode == 404) {
                FetchResult.Success(false)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
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

    suspend fun asyncEmailLogin(c: Credentials): FetchResult<Account> {
        return try {
            val account = withContext(Dispatchers.IO) {
                emailLogin(c)
            }

            if (account == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(account)
            }
        } catch (e: APIError) {
            FetchResult.ofLoginError(e)
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    /**
     * Create a new account form multiple purposes:
     * - Signup with email directly
     * - A mobile phone user is trying to login for the first time and
     * choose to create a new email account with mobile linked.
     */
    private fun emailSignUp(c: Credentials): Account? {
        return Fetch()
            .post(Endpoint.emailSignUp)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(c)
            .endJson<Account>()
            .body
    }

    suspend fun asyncEmailSignUp(c: Credentials): FetchResult<Account> {
        return try {
            val account = withContext(Dispatchers.IO) {
                emailSignUp(c)
            }

            if (account == null) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.Success(account)
            }
        } catch (e: APIError) {
            FetchResult.ofSignUpError(e)
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    private fun requestSMSCode(params: SMSCodeParams): Boolean {
        val resp = Fetch()
            .put(Endpoint.mobileVerificationCode)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(params)
            .endOrThrow()

        return resp.code == 204
    }

    suspend fun asyncRequestSMSCode(params: SMSCodeParams): FetchResult<Boolean> {
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

    private fun verifySMSCode(params: MobileAuthParams): MobileEmailLinked? {
        return Fetch()
            .post(Endpoint.mobileVerificationCode)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(params)
            .endJson<MobileEmailLinked>()
            .body
    }

    suspend fun asyncVerifySMSCode(params: MobileAuthParams): FetchResult<MobileEmailLinked> {
        try {
            val found = withContext(Dispatchers.IO) {
                verifySMSCode(params)
            } ?: return FetchResult.unknownError

            return FetchResult.Success(found)
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    private fun mobileLinkExistingEmail(params: MobileLinkParams): Account? {
        return Fetch()
            .post(Endpoint.mobileInitialLink)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(params)
            .endJson<Account>()
            .body
    }

    suspend fun asyncMobileLinkEmail(params: MobileLinkParams): FetchResult<Account> {
        return try {
            val account = withContext(Dispatchers.IO) {
                mobileLinkExistingEmail(params)
            }
            if (account == null) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.Success(account)
            }
        } catch (e: APIError) {
            FetchResult.ofLoginError(e)
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    /**
     * Create a new account with email derived from phone number.
     */
    private fun mobileSignUp(params: MobileSignUpParams): Account? {
        return Fetch()
            .post(Endpoint.mobileSignUp)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(params)
            .endJson<Account>()
            .body
    }

    suspend fun asyncMobileSignUp(params: MobileSignUpParams): FetchResult<Account> {
        try {
            val account = withContext(Dispatchers.IO) {
                mobileSignUp(params)
            }

            return if (account == null) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.Success(account)
            }
        } catch (e: APIError) {

            return when(e.statusCode) {
                422 -> {
                    if (e.error == null) {
                        FetchResult.fromApi(e)
                    } else {
                        when {
                            e.error.isFieldAlreadyExists("email") -> FetchResult.LocalizedError(R.string.signup_mobile_taken)
                            e.error.isFieldInvalid("mobile") -> FetchResult.LocalizedError(R.string.signup_invalid_mobile)
                            else -> FetchResult.fromApi(e)
                        }
                    }
                }
                else -> FetchResult.fromException(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    private fun passwordResetLetter(params: PasswordResetLetterParams): Boolean {
        val resp = Fetch()
            .post(Endpoint.passwordResetLetter)
            .setTimeout(30)
            .setClient()
            .noCache()
            .setApiKey()
            .sendJson(params)
            .endOrThrow()

        return resp.code == 204
    }
    suspend fun asyncPasswordResetLetter(params: PasswordResetLetterParams): FetchResult<Boolean> {
        return try {
            val ok = withContext(Dispatchers.IO) {
                passwordResetLetter(params)
            }

            FetchResult.Success(ok)
        } catch (e: APIError) {
            when (e.statusCode) {
                404 -> FetchResult.LocalizedError(R.string.login_email_not_found)
                else -> FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    fun verifyPwResetCode(v: PasswordResetVerifier): PwResetBearer? {
        return Fetch()
            .get("${Endpoint.passwordResetCodes}?email=${v.email}&code=${v.code}")
            .noCache()
            .setApiKey()
            .endJson<PwResetBearer>()
            .body
    }

    suspend fun asyncVerifyPwResetCode(params: PasswordResetVerifier): FetchResult<PwResetBearer> {
        try {
            val bearer = withContext(Dispatchers.IO) {
                verifyPwResetCode(params)
            } ?: return FetchResult.loadingFailed

            return FetchResult.Success(bearer)
        } catch (e: APIError) {
            return when (e.statusCode) {
                404 -> FetchResult.LocalizedError(R.string.forgot_password_code_not_found)
                else -> FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun resetPassword(params: PasswordResetParams): Boolean {
        val resp = Fetch()
            .post(Endpoint.passwordReset)
            .setClient()
            .noCache()
            .setApiKey()
            .sendJson(params)
            .endOrThrow()

        return resp.code == 204
    }

    suspend fun asyncResetPassword(params: PasswordResetParams): FetchResult<Boolean> {
        return try {
            val ok = withContext(Dispatchers.IO) {
                resetPassword(params)
            }

            FetchResult.Success(ok)
        } catch (e: APIError) {
            when (e.statusCode) {
                404 -> FetchResult.LocalizedError(R.string.forgot_password_code_not_found)
                else -> FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    fun ssoLogin(token: String): Account? {
        return Fetch()
            .post(Endpoint.tokenSso)
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(mapOf("token" to token))
            .endJson<Account>()
            .body
    }

    suspend fun asyncSsoLogin(token: String): FetchResult<Account> {
        return try {
            val account = withContext(Dispatchers.IO) {
                ssoLogin(token)
            } ?: return FetchResult.loadingFailed

            FetchResult.Success(account)
        } catch (e: APIError) {
            FetchResult.fromApi(e)
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    /**
     * Wecaht login does not return an [Account] instance.
     * It returns a [WxSession] which represents the access
     * token acquired from Wechat API.
     * Then you can use [WxSession] to retrieve Wechat
     * info or refresh account data.
     */
    private fun wxLogin(params: WxAuthParams): WxSession? {
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
            if (e is APIError) {
                Log.e(
                    "AuthClient",
                    "Wechat login API error: status=${e.statusCode}, message=${e.message}, error=${e.error}",
                    e
                )
            } else {
                Log.e("AuthClient", "Wechat login exception: ${e.message}", e)
            }
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
