package com.ft.ftchinese.repository

import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.*
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


object SubRepo : AnkoLogger {

    fun previewUpgrade(account: Account): PaymentIntent? {

        val (_, body) = account.createFetch()
                .get(SubscribeApi.UPGRADE_PREVIEW)
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

        val (resp, body) = account.createFetch()
                .put(SubscribeApi.UPGRADE)
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

        val (_, body) = account.createFetch()
                .get(NextApi.ORDERS)
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

        val (_, body) = account.createFetch()
            .post("${SubscribeApi.wxOrderUrl(isTest)}/${plan.tier}/${plan.cycle}")
            .setTest(isTest)
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

    fun wxQueryOrder(account: Account, orderId: String): PaymentResult? {

        val (_, body) = Fetch()
                .post(SubscribeApi.verifyPaymentUrl(orderId, account.isTest))
                .noCache()
                .setAppId() // Deprecated
                .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<PaymentResult>(body)
        }
    }

    fun aliPlaceOrder(account: Account, plan: Plan): AliPayIntent? {

        val isTest = account.isTest

        info("Is test pay $isTest")

        val (_, body) = account.createFetch()
            .post("${SubscribeApi.aliOrderUrl(isTest)}/${plan.tier}/${plan.cycle}")
            .setTest(isTest)
            .setTimeout(30)
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

    fun verifyPayment(account: Account, orderId: String):  PaymentResult? {
        val (_, body) = Fetch()
            .post(SubscribeApi.verifyPaymentUrl(orderId, account.isTest))
            .noCache()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<PaymentResult>(body)
        }
    }
}
