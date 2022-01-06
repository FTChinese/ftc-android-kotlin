package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.model.ftcsubs.VerificationResult
import com.ft.ftchinese.model.ftcsubs.WxPayIntent
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.request.OrderParams
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object FtcPayClient : AnkoLogger {

    fun listOrders(account: Account): List<Order> {

        val (_, body) = Fetch()
            .get(Endpoint.subsBase(account.isTest) + "/orders")
            .addHeaders(account.headers())
            .noCache()
            .endJsonText()

        return if (body == null) {
            listOf()
        } else {
            json.parseArray(body)
        } ?: listOf()
    }

    fun verifyOrder(account: Account, orderId: String):  VerificationResult? {
        val (_, body) = Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/orders/$orderId/verify-payment")
            .noCache()
            .sendJson()
            .endJsonText()

        info("Raw verification response $body")
        return if (body == null) {
            null
        } else {
            json.parse<VerificationResult>(body)
        }
    }

    fun createWxOrder(account: Account, params: OrderParams): WxPayIntent? {

        val (_, body) = Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/wxpay/app")
            .addHeaders(account.headers())
            .setTimeout(30)
            .noCache()
            .setClient()
            .setAppId() // Deprecated
            .sendJson(params.toJsonString())
            .endJsonText()

        info("Wx order: $body")

        return if (body == null) {
            null
        } else {
            json.parse<WxPayIntent>(body)
        }
    }

    fun createAliOrder(account: Account, params: OrderParams): AliPayIntent? {

        val (_, body) = Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/alipay/app")
            .setTimeout(30)
            .addHeaders(account.headers())
            .noCache()
            .setClient()
            .sendJson(params.toJsonString())
            .endJsonText()

        return if (body == null) {
            info("Ali order response no body")
            null
        } else {
            info("Parse ali order response $body")
            json.parse<AliPayIntent>(body)
        }
    }

    // Request api to add add-on to expiration date.
    fun useAddOn(account: Account): Membership? {
        val (_, body) = Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/membership/addons")
            .addHeaders(account.headers())
            .noCache()
            .sendJson()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<Membership>(body)
        }
    }
}
