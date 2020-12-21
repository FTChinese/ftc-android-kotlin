package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.*
import com.ft.ftchinese.model.fetch.json
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

    fun createWxOrder(account: Account, plan: Plan): WxPayIntent? {

        val (_, body) = Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/wxpay/app")
            .addHeaders(account.headers())
            .setTimeout(30)
            .noCache()
            .setClient()
            .setAppId() // Deprecated
            .sendJson(json.toJsonString(Edition(
                tier = plan.tier,
                cycle = plan.cycle
            )))
            .endJsonText()

        info("Wx order: $body")

        return if (body == null) {
            null
        } else {
            json.parse<WxPayIntent>(body)
        }
    }

    fun createAliOrder(account: Account, plan: Plan): AliPayIntent? {

        val (_, body) = Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/alipay/app")
            .setTimeout(30)
            .addHeaders(account.headers())
            .noCache()
            .setClient()
            .sendJson(json.toJsonString(Edition(
                tier = plan.tier,
                cycle = plan.cycle
            )))
            .endJsonText()

        return if (body == null) {
            info("Ali order response no body")
            null
        } else {
            info("Parse ali order response $body")
            json.parse<AliPayIntent>(body)
        }
    }

    fun previewUpgrade(account: Account): Checkout? {

        val (_, body) = Fetch()
            .get(Endpoint.subsBase(account.isTest) + "/upgrade/balance")
            .addHeaders(account.headers())
            .setTimeout(30)
            .noCache()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<Checkout>(body)
        }
    }

    fun directUpgrade(account: Account): Pair<Boolean, Checkout?> {

        val (resp, body) = Fetch()
            .put(Endpoint.subsBase(account.isTest) + "/upgrade/free")
            .addHeaders(account.headers())
            .noCache()
            .setClient()
            .endJsonText()

        return when (resp.code) {
            204 -> Pair(true, null)
            200 -> if (body != null) {
                try {
                    Pair(false, json.parse<Checkout>(body))
                } catch (e: Exception) {
                    info(e)
                    Pair(false, null)
                }
            } else {
                Pair(false, null)
            }
            else -> Pair(false, null)
        }
    }

    fun refreshIAP(account: Account): IAPSubs? {

        val origTxId = account.membership.appleSubsId ?: throw Exception("Not an Apple subscription")

        val (_, body) = Fetch()
            .patch(Endpoint.subsBase(account.isTest) + "/apple/subs/$origTxId")
            .noCache()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<IAPSubs>(body)
        }
    }
}
