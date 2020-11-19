package com.ft.ftchinese.repository

import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.*
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


object SubRepo : AnkoLogger {

    fun previewUpgrade(account: Account): PaymentIntent? {

        val (_, body) = Fetch()
            .get(SubscribeApi.UPGRADE_PREVIEW)
            .addHeaders(account.headers())
            .setTimeout(30)
            .noCache()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<PaymentIntent>(body)
        }
    }

    fun directUpgrade(account: Account): Pair<Boolean, PaymentIntent?> {

        val (resp, body) = Fetch()
            .put(SubscribeApi.UPGRADE)
            .addHeaders(account.headers())
            .noCache()
            .setClient()
            .endJsonText()

        return when (resp.code) {
            204 -> Pair(true, null)
            200 -> if (body != null) {
                try {
                    Pair(true, json.parse<PaymentIntent>(body))
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

    fun getOrders(account: Account): List<Order> {

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

    fun wxPlaceOrder(account: Account, plan: Plan): WxPayIntent? {

        // If current account is a testing one, always send request to sandbox.
        val isTest = account.isTest

        val (_, body) = Fetch()
            .post("${SubscribeApi.wxOrderUrl(isTest)}/${plan.tier}/${plan.cycle}")
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

    fun aliPlaceOrder(account: Account, plan: Plan): AliPayIntent? {

        val isTest = account.isTest

        info("Is test pay $isTest")

        val (_, body) = Fetch()
            .post("${SubscribeApi.aliOrderUrl(isTest)}/${plan.tier}/${plan.cycle}")
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

    fun verifyPayment(account: Account, orderId: String):  VerificationResult? {
        val (_, body) = Fetch()
            .post(SubscribeApi.verifyPaymentUrl(orderId, account.isTest))
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

    fun refreshIAP(account: Account): IAPSubs? {
        if (account.membership.appleSubsId == null) {
            return null
        }

        val (_, body) = Fetch()
            .patch(SubscribeApi.refreshIAP(account.membership.appleSubsId, account.isTest))
            .noCache()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<IAPSubs>(body)
        }
    }
}
