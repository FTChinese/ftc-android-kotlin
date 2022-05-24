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
import com.ft.ftchinese.model.request.EmailPasswordParams
import com.ft.ftchinese.model.request.MobileFormParams
import com.ft.ftchinese.model.request.PasswordUpdateParams
import com.ft.ftchinese.model.request.SMSCodeParams
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
                AccountRepo.refresh(account)
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

    fun updateUserName(ftcId: String, name: String): BaseAccount? {
        return Fetch().patch(Endpoint.userName)
            .noCache()
            .setApiKey()
            .setUserId(ftcId)
            .sendJson(mapOf("userName" to name))
            .endJson<BaseAccount>()
            .body
    }

    fun updatePassword(ftcId: String, params: PasswordUpdateParams): Boolean {
        val resp =  Fetch()
            .patch(Endpoint.passwordUpdate)
            .noCache()
            .setApiKey()
            .setUserId(ftcId)
            .sendJson(params)
            .endText()

        return resp.code == 204
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

    fun requestSMSCode(account: Account, params: SMSCodeParams): Boolean {
        val resp = Fetch()
            .put("${Endpoint.subsBase(account.isTest)}/account/mobile/verification")
            .noCache()
            .setApiKey()
            .setClient()
            .setUserId(account.id)
            .sendJson(params)
            .endText()

        return resp.code == 204
    }

    fun updateMobile(account: Account, params: MobileFormParams): BaseAccount? {
        return Fetch()
            .patch("${Endpoint.subsBase(account.isTest)}/account/mobile")
            .noCache()
            .setApiKey()
            .setClient()
            .setUserId(account.id)
            .sendJson(params)
            .endJson<BaseAccount>()
            .body
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

    fun updateAddress(ftcId: String, address: Address): Boolean {
        val resp = Fetch()
            .patch(Endpoint.address)
            .setUserId(ftcId)
            .noCache()
            .setApiKey()
            .sendJson(address)
            .endText()

        return resp.code == 204
    }

    fun deleteAccount(ftcId: String, params: EmailPasswordParams): Boolean {
        val resp =  Fetch()
            .delete(Endpoint.ftcAccount)
            .noCache()
            .setApiKey()
            .setUserId(ftcId)
            .sendJson(params)
            .endText()

        return resp.code == 204
    }
}
