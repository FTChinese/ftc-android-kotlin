package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.VerificationResult
import com.ft.ftchinese.model.ftcsubs.WxPayIntent
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.request.OrderParams

object FtcPayClient {

    fun verifyOrder(account: Account, orderId: String):  VerificationResult? {
        return Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/orders/$orderId/verify-payment")
            .noCache()
            .setApiKey()
            .send()
            .endJson<VerificationResult>()
            .body
    }

    fun createWxOrder(account: Account, params: OrderParams): WxPayIntent? {

        return Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/wxpay/app")
            .addHeaders(account.headers())
            .setTimeout(30)
            .noCache()
            .setClient()
            .setApiKey()
            .setAppId() // Deprecated
            .sendJson(params)
            .endJson<WxPayIntent>()
            .body
    }

    fun createAliOrder(account: Account, params: OrderParams): AliPayIntent? {

        return Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/alipay/app")
            .setTimeout(30)
            .addHeaders(account.headers())
            .noCache()
            .setApiKey()
            .setClient()
            .sendJson(params)
            .endJson<AliPayIntent>()
            .body
    }

    // Request api to add add-on to expiration date.
    fun useAddOn(account: Account): Membership? {
       return Fetch()
           .post(Endpoint.subsBase(account.isTest) + "/membership/addons")
           .addHeaders(account.headers())
           .noCache()
           .setApiKey()
           .send()
           .endJson<Membership>()
           .body
    }
}
