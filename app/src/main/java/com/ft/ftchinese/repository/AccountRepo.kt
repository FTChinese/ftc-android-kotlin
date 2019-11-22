package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.Passwords
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.SubscribeApi
import com.ft.ftchinese.util.json

object AccountRepo {
    fun loadFtcAccount(ftcId: String): Account? {
        val(_, body) = Fetch().get(NextApi.ACCOUNT)
                .noCache()
                .setUserId(ftcId)
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
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

    fun refresh(account: Account): Account? {
        return when (account.loginMethod) {
            LoginMethod.EMAIL,
            LoginMethod.MOBILE -> loadFtcAccount(account.id)

            LoginMethod.WECHAT -> if (account.unionId != null) {
                loadFtcAccount(account.unionId)
            } else {
                null
            }
            else -> null
        }
    }

    fun updateEmail(ftcId: String, email: String): Boolean {
        val (resp, _) = Fetch().patch(NextApi.UPDATE_EMAIL)
                .noCache()
                .setUserId(ftcId)
                .jsonBody(json.toJsonString(mapOf("email" to email)))
                .responseApi()

        return resp.code == 204
    }

    fun updateUserName(ftcId: String, name: String): Boolean {
        val (resp, _) = Fetch().patch(NextApi.UPDATE_USER_NAME)
                .noCache()
                .setUserId(ftcId)
                .jsonBody(json.toJsonString(mapOf("userName" to name)))
                .responseApi()

        return resp.code == 204
    }

    fun updatePassword(ftcId: String, pw: Passwords): Boolean {
        val(resp, _) = Fetch().patch(NextApi.UPDATE_PASSWORD)
                .noCache()
                .setUserId(ftcId)
                .jsonBody(json.toJsonString(pw))
                .responseApi()

        return resp.code == 204
    }

    fun requestVerification(ftcId: String): Boolean {
        val (resp, _) = Fetch()
                .post(NextApi.REQUEST_VERIFICATION)
                .setTimeout(30)
                .noCache()
                .setClient()
                .setUserId(ftcId)
                .body()
                .responseApi()

        return resp.code == 204
    }

    /**
     * Asks the API to refresh current wechat user's access token and information.
     */
    fun refreshWxInfo(wxSession: WxSession): Boolean {
        val (resp, _) = Fetch().put(SubscribeApi.WX_REFRESH)
                .noCache()
                .setAppId()
                .jsonBody(Klaxon().toJsonString(mapOf(
                        "sessionId" to wxSession.sessionId
                )))
                .responseApi()

        return resp.code == 204
    }

    fun loadWxAvatar(url: String): ByteArray? {

        return Fetch()
                .get(url)
                .download()
    }
}
