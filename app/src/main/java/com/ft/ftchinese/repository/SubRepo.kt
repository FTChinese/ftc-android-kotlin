package com.ft.ftchinese.repository


import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.PaymentIntent
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.SubscribeApi
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object SubRepo : AnkoLogger {
    fun previewUpgrade(account: Account): PaymentIntent? {

        val (_, body) = account.createFetch()
                .get(SubscribeApi.UPGRADE_PREVIEW)
                .setTimeout(30)
                .noCache()
                .responseApi()

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
                .responseApi()

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
                .responseApi()

        return if (body == null) {
            listOf()
        } else {
            json.parseArray(body)
        } ?: listOf()
    }

    fun wxPlaceOrder(account: Account, plan: Plan): WxOrder? {

        val (_, body) = account.createFetch()
                .post("${SubscribeApi.WX_UNIFIED_ORDER}/${plan.tier}/${plan.cycle}")
                .setTimeout(30)
                .noCache()
                .setClient()
                .setAppId()
                .body()
                .responseApi()

        return if (body == null) {
            null
        } else {
            try {
                json.parse<WxOrder>(body)
            } catch (e: Exception) {
                info(e)

                null
            }
        }
    }

    fun wxQueryOrder(account: Account, orderId: String): WxPaymentStatus? {

        val (_, body) = account.createFetch()
                .get("${SubscribeApi.WX_ORDER_QUERY}/$orderId")
                .noCache()
                .setAppId()
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<WxPaymentStatus>(body)
        }
    }

    fun aliPlaceOrder(account: Account, plan: Plan): AliOrder? {

        val (_, body) = account.createFetch()
                .post("${SubscribeApi.ALI_ORDER}/${plan.tier}/${plan.cycle}")
                .setTimeout(30)
                .noCache()
                .setClient()
                .body()
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<AliOrder>(body)
        }
    }

//    fun createCustomer(id: String): String? {
//        val (_, body) = Fetch()
//                .put(SubscribeApi.STRIPE_CUSTOMER)
//                .setUserId(id)
//                .noCache()
//                .body()
//                .responseApi()
//
//        if (body == null) {
//            return null
//        }
//
//        return try {
//            JSONObject(body).getString("id")
//        } catch (e: JSONException) {
//            null
//        }
//    }

//    fun createEphemeralKey(account: Account, apiVersion: String): String? {
//        if (account.stripeId == null) {
//            return null
//        }
//
//        val (_, body) = Fetch()
//                .post("${SubscribeApi.STRIPE_CUSTOMER}/${account.stripeId}/ephemeral_keys")
//                .setUserId(account.id)
//                .query("api_version", apiVersion)
//                .noCache()
//                .body()
//                .responseApi()
//
//        return body
//    }

//    fun getStripePlan(id: String): StripePlan? {
//        val (_, body) = Fetch()
//                .get("${SubscribeApi.STRIPE_PLAN}/$id")
//                .setUserId(id)
//                .responseApi()
//
//        return if (body == null) {
//            return null
//        } else {
//            json.parse<StripePlan>(body)
//        }
//    }

//    fun createSubscription(account: Account, params: StripeSubParams): StripeSubResponse? {
//
//        val fetch = Fetch()
//                .post(SubscribeApi.STRIPE_SUB)
//                .setUserId(account.id)
//                .noCache()
//                .jsonBody(json.toJsonString(params))
//
//        if (account.unionId != null) {
//            fetch.setUnionId(account.unionId)
//        }
//
//        val (_, body ) = fetch.responseApi()
//
//        return if (body == null) {
//            null
//        } else {
//            json.parse<StripeSubResponse>(body)
//        }
//    }

//    fun refreshStripeSub(account: Account): StripeSub? {
//        val fetch = Fetch()
//                .get(SubscribeApi.STRIPE_SUB)
//                .setUserId(account.id)
//                .noCache()
//
//        if (account.unionId != null) {
//            fetch.setUnionId(account.unionId)
//        }
//
//        val (_, body) = fetch.responseApi()
//
//        return if (body == null) {
//            null
//        } else {
//            json.parse(body)
//        }
//    }

//    fun upgradeStripeSub(account: Account, params: StripeSubParams): StripeSubResponse? {
//        val fetch = Fetch()
//                .patch(SubscribeApi.STRIPE_SUB)
//                .setUserId(account.id)
//                .noCache()
//                .jsonBody(json.toJsonString(params))
//
//        if (account.unionId != null) {
//            fetch.setUnionId(account.unionId)
//        }
//
//        val (_, body) = fetch.responseApi()
//        return if (body == null) {
//            null
//        } else {
//            json.parse(body)
//        }
//    }
}
