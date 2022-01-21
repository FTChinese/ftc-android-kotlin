package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.request.*

object AccountRepo {
    fun loadFtcAccount(ftcId: String): Account? {
        return Fetch()
            .get(Endpoint.ftcAccount)
            .noCache()
            .setUserId(ftcId)
            .endApiJson<Account>()
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
            .endApiJson<Account>()
            .body
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

    fun updateEmail(ftcId: String, email: String): BaseAccount? {
        return Fetch().patch(Endpoint.email)
            .noCache()
            .setUserId(ftcId)
            .sendJson(json.toJsonString(mapOf("email" to email)))
            .endApiJson<BaseAccount>()
            .body
    }

    fun updateUserName(ftcId: String, name: String): BaseAccount? {
        return Fetch().patch(Endpoint.userName)
            .noCache()
            .setUserId(ftcId)
            .sendJson(json.toJsonString(mapOf("userName" to name)))
            .endApiJson<BaseAccount>()
            .body
    }

    fun updatePassword(ftcId: String, params: PasswordUpdateParams): Boolean {
        val resp =  Fetch()
            .patch(Endpoint.passwordUpdate)
            .noCache()
            .setUserId(ftcId)
            .sendJson(params.toJsonString())
            .endApiText()

        return resp.code == 204
    }

    fun requestVerification(ftcId: String): Boolean {
        val resp = Fetch()
            .post(Endpoint.emailVrfLetter)
            .setTimeout(30)
            .noCache()
            .setClient()
            .setUserId(ftcId)
            .sendJson()
            .endApiText()

        return resp.code == 204
    }

    fun requestSMSCode(account: Account, params: SMSCodeParams): Boolean {
        val resp = Fetch()
            .put("${Endpoint.subsBase(account.isTest)}/account/mobile/verification")
            .noCache()
            .setClient()
            .setUserId(account.id)
            .sendJson(params.toJsonString())
            .endApiText()

        return resp.code == 204
    }

    fun updateMobile(account: Account, params: MobileFormParams): BaseAccount? {
        return Fetch()
            .patch("${Endpoint.subsBase(account.isTest)}/account/mobile")
            .noCache()
            .setClient()
            .setUserId(account.id)
            .sendJson(params.toJsonString())
            .endApiJson<BaseAccount>()
            .body
    }

    /**
     * Asks the API to refresh current wechat user's access token and information.
     */
    fun refreshWxInfo(wxSession: WxSession): Boolean {
        val resp = Fetch()
            .post(Endpoint.wxRefresh)
            .noCache()
            .setAppId()
            .setTimeout(30)
            .sendJson(Klaxon().toJsonString(mapOf(
                "sessionId" to wxSession.sessionId
            )))
            .endApiText()

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
           .endApiJson<Address>()
           .body
    }

    fun updateAddress(ftcId: String, address: Address): Boolean {
        val resp = Fetch()
            .patch(Endpoint.address)
            .setUserId(ftcId)
            .noCache()
            .sendJson(json.toJsonString(address))
            .endApiText()

        return resp.code == 204
    }

    fun deleteAccount(ftcId: String, params: EmailPasswordParams): Boolean {
        val resp =  Fetch()
            .delete(Endpoint.ftcAccount)
            .noCache()
            .setUserId(ftcId)
            .sendJson(params.toJsonString())
            .endApiText()

        return resp.code == 204
    }
}
