package com.ft.ftchinese.repository

import com.ft.ftchinese.model.order.StripePlan
import com.ft.ftchinese.model.order.StripeSub
import com.ft.ftchinese.model.order.StripeSubParams
import com.ft.ftchinese.model.order.StripeSubResponse
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.SubscribeApi
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger
import org.json.JSONException

object StripeRepo : AnkoLogger {
    fun createCustomer(id: String): StripeCustomer? {
        val (_, body) = Fetch()
                .put(SubscribeApi.STRIPE_CUSTOMER)
                .setUserId(id)
                .noCache()
                .body()
                .responseApi()

        if (body == null) {
            return null
        }

        return try {
            json.parse<StripeCustomer>(body)
        } catch (e: JSONException) {
            null
        }
    }

    fun createEphemeralKey(account: Account, apiVersion: String): String? {
        if (account.stripeId == null) {
            return null
        }

        val (_, body) = Fetch()
                .post("${SubscribeApi.STRIPE_CUSTOMER}/${account.stripeId}/ephemeral_keys")
                .setUserId(account.id)
                .query("api_version", apiVersion)
                .noCache()
                .body()
                .responseApi()

        return body
    }

    fun getStripePlan(id: String): StripePlan? {
        val (_, body) = Fetch()
                .get("${SubscribeApi.STRIPE_PLAN}/$id")
                .setUserId(id)
                .responseApi()

        return if (body == null) {
            return null
        } else {
            json.parse<StripePlan>(body)
        }
    }

    fun createSubscription(account: Account, params: StripeSubParams): StripeSubResponse? {

        val fetch = Fetch()
                .post(SubscribeApi.STRIPE_SUB)
                .setUserId(account.id)
                .noCache()
                .jsonBody(json.toJsonString(params))

        if (account.unionId != null) {
            fetch.setUnionId(account.unionId)
        }

        val (_, body ) = fetch.responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<StripeSubResponse>(body)
        }
    }

    // Ask API to update user's Stripe subscription data.
    fun refreshStripeSub(account: Account): StripeSub? {
        val fetch = Fetch()
                .get(SubscribeApi.STRIPE_SUB)
                .setUserId(account.id)
                .noCache()

        if (account.unionId != null) {
            fetch.setUnionId(account.unionId)
        }

        val (_, body) = fetch.responseApi()

        return if (body == null) {
            null
        } else {
            json.parse(body)
        }
    }

    fun upgradeStripeSub(account: Account, params: StripeSubParams): StripeSubResponse? {
        val fetch = Fetch()
                .patch(SubscribeApi.STRIPE_SUB)
                .setUserId(account.id)
                .noCache()
                .jsonBody(json.toJsonString(params))

        if (account.unionId != null) {
            fetch.setUnionId(account.unionId)
        }

        val (_, body) = fetch.responseApi()
        return if (body == null) {
            null
        } else {
            json.parse(body)
        }
    }
}
