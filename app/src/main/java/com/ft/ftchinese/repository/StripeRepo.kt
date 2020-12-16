package com.ft.ftchinese.repository

import com.ft.ftchinese.model.order.StripeSubParams
import com.ft.ftchinese.model.order.StripeSubResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.model.subscription.StripePrice
import com.ft.ftchinese.util.json
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.json.JSONException

object StripeRepo : AnkoLogger {
    fun createCustomer(account: Account): StripeCustomer? {
        val (_, body) = Fetch()
            .post(SubscribeApi.stripeSubBase(account.isTest))
            .setUserId(account.id)
            .noCache()
            .sendJson()
            .endJsonText()

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
            .sendJson()
            .endJsonText()

        return body
    }

    fun loadPlan(id: String): StripePrice? {
        val (_, body) = Fetch()
            .get("${SubscribeApi.STRIPE_PLAN}/$id")
            .setUserId(id)
            .endJsonText()

        return if (body == null) {
            return null
        } else {
            json.parse<StripePrice>(body)
        }
    }

    fun createSubscription(account: Account, params: StripeSubParams): StripeSubResult? {

        val (_, body ) = Fetch()
            .post(SubscribeApi.stripeSubBase(account.isTest))
            .setUserId(account.id)
            .noCache()
            .sendJson(json.toJsonString(params))
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<StripeSubResult>(body)
        }
    }

    // Ask API to update user's Stripe subscription data.
    fun refreshSub(account: Account): StripeSubResult? {

        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val (_, body) = Fetch()
            .get(SubscribeApi.stripeRefresh(subsId, account.isTest))
            .setUserId(account.id)
            .noCache()
            .endJsonText()

        info(body)

        return if (body == null) {
            null
        } else {
            json.parse(body)
        }
    }

    fun upgradeSub(account: Account, params: StripeSubParams): StripeSubResult? {

        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val (_, body) = Fetch()
            .post(SubscribeApi.stripeUpgrade(subsId, account.isTest))
            .setUserId(account.id)
            .noCache()
            .sendJson(json.toJsonString(params))
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse(body)
        }
    }

    fun cancelSub(account: Account): StripeSubResult? {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val (_, body) = Fetch()
            .post(SubscribeApi.stripeCancel(subsId, account.isTest))
            .setUserId(account.id)
            .noCache()
            .sendJson()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse(body)
        }
    }

    fun reactivateSub(account: Account): StripeSubResult? {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val (_, body) = Fetch()
            .post(SubscribeApi.stripeReactivate(subsId, account.isTest))
            .setUserId(account.id)
            .noCache()
            .sendJson()
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse(body)
        }
    }
}
