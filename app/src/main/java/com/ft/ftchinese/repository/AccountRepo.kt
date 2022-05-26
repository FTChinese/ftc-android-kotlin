package com.ft.ftchinese.repository

import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.LoginMethod
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.model.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AccountRepo {

    fun loadFtcAccount(ftcId: String): Account? {
        return Fetch()
            .get(Endpoint.ftcAccount)
            .noCache()
            .setApiKey()
            .setUserId(ftcId)
            .endJson<Account>()
            .body
    }

    /**
     * Account retrieved from here always has loginMethod set to `wechat`.
     * Only used for initial login.
     * DO NOT use this to refresh account data since WxSession only exists
     * if user logged in via wechat OAuth.
     * If user logged in with email + password (and the the email is bound to this wechat),
     * WxSession never actually exists.
     */
    fun loadWxAccount(unionId: String): Account? {
        return Fetch()
            .get(Endpoint.wxAccount)
            .setUnionId(unionId)
            .noCache()
            .setApiKey()
            .endJson<Account>()
            .body
    }

    suspend fun asyncLoadWxAccount(unionId: String): FetchResult<Account> {
        try {
            val account = withContext(Dispatchers.IO) {
                loadWxAccount(unionId)
            } ?: return FetchResult.loadingFailed

            return FetchResult.Success(account)
        } catch (e: APIError) {

            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.fromApi(e)
            }

        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun refresh(account: Account): Account? {
        return when (account.loginMethod) {
            LoginMethod.EMAIL,
            LoginMethod.MOBILE -> loadFtcAccount(account.id)

            LoginMethod.WECHAT -> if (account.unionId != null) {
                loadWxAccount(account.unionId)
            } else {
                null
            }
            else -> null
        }
    }

    suspend fun asyncRefresh(account: Account): FetchResult<Account> {
        try {

            val updatedAccount = withContext(Dispatchers.IO) {
                refresh(account)
            } ?: return FetchResult.LocalizedError(R.string.loading_failed)

            return FetchResult.Success(updatedAccount)
        } catch (e: APIError) {

            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.account_not_found)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun updateEmail(ftcId: String, email: String): BaseAccount? {
        return Fetch().patch(Endpoint.email)
            .noCache()
            .setApiKey()
            .setUserId(ftcId)
            .sendJson(mapOf("email" to email))
            .endJson<BaseAccount>()
            .body
    }

    suspend fun asyncUpdateEmail(ftcId: String, email: String): FetchResult<BaseAccount> {
        try {
            val baseAccount = withContext(Dispatchers.IO) {
                updateEmail(ftcId, email)
            }

            return if (baseAccount == null) {
                FetchResult.LocalizedError(R.string.error_unknown)
            } else {
                FetchResult.Success(baseAccount)
            }
        } catch (e: APIError) {
            return when (e.statusCode) {
                422 -> {
                    if (e.error == null) {
                        FetchResult.fromApi(e)
                    } else {
                        when {
                            e.error.isFieldAlreadyExists("email") -> FetchResult.LocalizedError(R.string.signup_email_taken)
                            e.error.isFieldInvalid("email") -> FetchResult.LocalizedError(R.string.signup_invalid_email)
                            else -> FetchResult.fromApi(e)
                        }
                    }
                }
                else -> FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun updateUserName(ftcId: String, name: String): BaseAccount? {
        return Fetch().patch(Endpoint.userName)
            .noCache()
            .setApiKey()
            .setUserId(ftcId)
            .sendJson(mapOf("userName" to name))
            .endJson<BaseAccount>()
            .body
    }

    suspend fun asyncUpdateName(ftcId: String, name: String): FetchResult<BaseAccount> {
        try {
            val baseAccount = withContext(Dispatchers.IO) {
                updateUserName(ftcId, name)
            }

            return if (baseAccount == null) {
                FetchResult.unknownError
            } else {
                FetchResult.Success(baseAccount)
            }
        } catch (e: APIError) {
            return if (e.statusCode == 422) {
                when (e.error?.key) {
                    "userName_already_exists" -> FetchResult.LocalizedError(R.string.api_name_taken)
                    else -> FetchResult.fromApi(e)
                }
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun updatePassword(ftcId: String, params: PasswordUpdateParams): Boolean {
        val resp =  Fetch()
            .patch(Endpoint.passwordUpdate)
            .noCache()
            .setApiKey()
            .setUserId(ftcId)
            .sendJson(params)
            .endOrThrow()

        return resp.code == 204
    }

    suspend fun asyncUpdatePassword(ftcId: String, params: PasswordUpdateParams): FetchResult<PwUpdateResult> {
        try {
            val done = withContext(Dispatchers.IO) {
                updatePassword(ftcId, params)
            }

            return if (done) {
                FetchResult.Success(PwUpdateResult.Done)
            } else {
                FetchResult.loadingFailed
            }
        } catch (e: APIError) {

            return when (e.statusCode) {
                403 -> FetchResult.Success(PwUpdateResult.Mismatched)
                404 -> FetchResult.LocalizedError(R.string.account_not_found)
                422 -> when {
                    e.error == null -> FetchResult.fromApi(e)
                    e.error.isFieldInvalid("password") -> FetchResult.LocalizedError(R.string.signup_invalid_password)
                    else -> FetchResult.fromApi(e)
                }
                else -> FetchResult.fromApi(e)
            }

        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun requestVerification(ftcId: String): Boolean {
        val resp = Fetch()
            .post(Endpoint.emailVrfLetter)
            .setTimeout(30)
            .noCache()
            .setApiKey()
            .setClient()
            .setUserId(ftcId)
            .send()
            .endText()

        return resp.code == 204
    }

    suspend fun asyncRequestVerification(ftcId: String): FetchResult<Boolean> {
        try {
            val done = withContext(Dispatchers.IO) {
                requestVerification(ftcId)
            }

            return FetchResult.Success(done)
        } catch (e: APIError) {
            return when (e.statusCode) {
                404 -> FetchResult.LocalizedError(R.string.account_not_found)
                422 -> if (e.error?.isResourceMissing("email_server") == true) {
                    FetchResult.LocalizedError(R.string.api_email_server_down)
                } else {
                    FetchResult.fromApi(e)
                }
                else -> {
                    FetchResult.fromApi(e)
                }
            }

        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun requestSMSCode(ftcId: String, params: SMSCodeParams): Boolean {
        val resp = Fetch()
            .put(Endpoint.smsCode)
            .noCache()
            .setApiKey()
            .setClient()
            .setUserId(ftcId)
            .sendJson(params)
            .endOrThrow()

        return resp.code == 204
    }

    suspend fun asyncRequestSMSCode(ftcId: String, params: SMSCodeParams): FetchResult<Boolean> {
        try {
            val ok = withContext(Dispatchers.IO) {
                requestSMSCode(
                    ftcId,
                    params
                )
            }

            return if (ok) {
                FetchResult.Success(true)
            } else {
                FetchResult.unknownError
            }
        } catch (e: APIError) {
            return when (e.statusCode) {
                404 -> FetchResult.accountNotFound
                422 -> if (e.error == null) {
                    FetchResult.fromApi(e)
                } else {
                    when {
                        // Alert this message
                        e.error.isFieldAlreadyExists("mobile") -> FetchResult.LocalizedError(R.string.mobile_conflict)
                        e.error.isFieldInvalid("mobile") -> FetchResult.LocalizedError(R.string.mobile_invalid)
                        else -> FetchResult.fromApi(e)
                    }
                }
                else -> FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun updateMobile(ftcId: String, params: MobileFormParams): BaseAccount? {
        return Fetch()
            .patch(Endpoint.updateMobile)
            .noCache()
            .setApiKey()
            .setClient()
            .setUserId(ftcId)
            .sendJson(params)
            .endJson<BaseAccount>()
            .body
    }

    suspend fun asyncUpdateMobile(ftcId: String, params: MobileFormParams): FetchResult<BaseAccount> {
        try {
            val baseAccount = withContext(Dispatchers.IO) {
                updateMobile(ftcId, params)
            }

            if (baseAccount == null) {
                return FetchResult.unknownError
            } else {
                return FetchResult.Success(baseAccount)
            }
        } catch (e: APIError) {
            return when (e.statusCode) {
                404 -> FetchResult.LocalizedError(R.string.mobile_code_not_found)
                422 -> if (e.error == null) {
                    FetchResult.fromApi(e)
                } else {
                    when {
                        e.error.isFieldAlreadyExists("mobile") -> FetchResult.LocalizedError(R.string.mobile_already_exists)
                        e.error.isFieldInvalid("mobile") -> FetchResult.LocalizedError(R.string.mobile_invalid)
                        e.error.isFieldInvalid("code") -> FetchResult.LocalizedError(R.string.mobile_code_invalid)
                        else -> FetchResult.fromApi(e)
                    }
                }
                else -> FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    /**
     * Asks the API to refresh current wechat user's access token and information.
     */
    fun refreshWxInfo(wxSession: WxSession): Boolean {
        val resp = Fetch()
            .post(Endpoint.wxRefresh)
            .noCache()
            .setApiKey()
            .setAppId()
            .setTimeout(30)
            .sendJson(mapOf(
                "sessionId" to wxSession.sessionId
            ))
            .endText()

        // The server API might change and return data in the future.
        return resp.code == 204 || resp.code == 200
    }

    suspend fun asyncRefreshWxInfo(sess: WxSession): FetchResult<Boolean> {
        return try {
            val done = withContext(Dispatchers.IO) {
                refreshWxInfo(wxSession = sess)
            }

            FetchResult.Success(done)
        } catch (e: APIError) {
            FetchResult.fromApi(e)
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    fun loadWxAvatar(url: String): ByteArray? {
        return Fetch()
            .get(url)
            .download()
    }

    fun loadAddress(ftcId: String): Address? {
       return Fetch()
           .get(Endpoint.address)
           .setUserId(ftcId)
           .noCache()
           .setApiKey()
           .endJson<Address>()
           .body
    }

    suspend fun asyncLoadAddress(ftcId: String): Address? {
        return try {
            withContext(Dispatchers.IO) {
                loadAddress(ftcId)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun updateAddress(ftcId: String, address: Address): Address? {
        return Fetch()
            .patch(Endpoint.address)
            .setUserId(ftcId)
            .noCache()
            .setApiKey()
            .sendJson(address)
            .endJson<Address>()
            .body
    }

    suspend fun asyncUpdateAddress(ftcId: String, address: Address): FetchResult<Address> {
        return try {
            val addr = withContext(Dispatchers.IO) {
                updateAddress(ftcId, address)
            }

            if (addr == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(addr)
            }
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    fun deleteAccount(ftcId: String, params: EmailPasswordParams): Boolean {
        val resp =  Fetch()
            .delete(Endpoint.ftcAccount)
            .noCache()
            .setApiKey()
            .setUserId(ftcId)
            .sendJson(params)
            .endOrThrow()

        return resp.code == 204
    }

    suspend fun asyncDeleteAccount(ftcId: String, params: EmailPasswordParams): FetchResult<AccountDropped> {
        try {
            val done = withContext(Dispatchers.IO) {
                deleteAccount(
                    ftcId = ftcId,
                    params = params,
                )
            }

            return if (done) {
                FetchResult.Success(AccountDropped.Success)
            } else {
                FetchResult.loadingFailed
            }

        } catch (e: APIError) {
            return when (e.statusCode) {
                403 -> {
                    FetchResult.LocalizedError(R.string.password_not_verified)
                }
                404 -> {
                    FetchResult.LocalizedError(R.string.account_not_found)
                }
                422 -> {
                    when {
                        e.error?.isFieldMissing("email") == true -> {
                            FetchResult.LocalizedError(R.string.message_delete_email_mismatch)
                        }
                        e.error?.isFieldAlreadyExists("subscription") == true -> {
                            FetchResult.Success(AccountDropped.SubsExists)
                        }
                        else -> {
                            FetchResult.fromApi(e)
                        }
                    }
                }
                else -> FetchResult.fromApi(e)
            }

        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }
}
