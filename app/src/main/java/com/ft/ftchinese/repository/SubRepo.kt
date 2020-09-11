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

    fun wxPlaceOrder(account: Account, plan: Plan): WxOrder? {

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
            json.parse<WxOrder>(body)
        }
    }

    fun wxQueryOrder(account: Account, orderId: String): WxPaymentStatus? {

        val (_, body) = account.createFetch()
                .get("${SubscribeApi.WX_ORDER_QUERY}/$orderId")
                .noCache()
                .setAppId() // Deprecated
                .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<WxPaymentStatus>(body)
        }
    }

    fun aliPlaceOrder(account: Account, plan: Plan): AliOrder? {

        val isTest = account.isTest

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

        info("Aliorder $body")

        return if (body == null) {
            null
        } else {
            json.parse<AliOrder>(body)
        }
    }
}
