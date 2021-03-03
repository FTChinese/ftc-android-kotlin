package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.ftcsubs.*
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.iapsubs.Subscription
import com.ft.ftchinese.model.price.Price
import com.ft.ftchinese.model.reader.Membership
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object SubRepo : AnkoLogger {

    fun listOrders(account: Account): List<Order> {

        val (_, body) = Fetch()
            .get(NextApi.ORDERS)
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

    fun createWxOrder(account: Account, price: Price): WxPayIntent? {

        val (_, body) = Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/wxpay/app")
            .addHeaders(account.headers())
            .setTimeout(30)
            .noCache()
            .setClient()
            .setAppId() // Deprecated
            .sendJson(json.toJsonString(
                price.edition
            ))
            .endJsonText()

        info("Wx order: $body")

        return if (body == null) {
            null
        } else {
            json.parse<WxPayIntent>(body)
        }
    }

    fun createAliOrder(account: Account, price: Price): AliPayIntent? {

        val (_, body) = Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/alipay/app")
            .setTimeout(30)
            .addHeaders(account.headers())
            .noCache()
            .setClient()
            .sendJson(json.toJsonString(
                price.edition
            ))
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
            .post(Endpoint.subsBase(account.isTest) + "/addon")
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
