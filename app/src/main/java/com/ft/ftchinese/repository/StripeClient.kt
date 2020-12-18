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

object StripeClient : AnkoLogger {

    private fun baseUrl(isTest: Boolean) =
        "${Endpoint.subsBase(isTest)}/stripe"

    // Retrieve a list of stripe prices.
    // If use is logged-in and it is a test account, use sandbox
    // api to get test prices; otherwise retrieve prices from live mode.
    fun listPrices(account: Account?): JSONResult<List<StripePrice>>? {
        val isTest = account?.isTest ?: false

        val (_, body) = Fetch()
            .get(baseUrl(isTest) + "/prices")
            .noCache()
            .endJsonText()

        if (body == null) {
            return null
        }

        val prices = json.parseArray<StripePrice>(body)
        return if (prices == null) {
            null
        } else {
            JSONResult(prices, body)
        }
    }

    fun createCustomer(account: Account): StripeCustomer? {
        val (_, body) = Fetch()
            .post(baseUrl(account.isTest) + "/customers")
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
            .post(baseUrl(account.isTest) + "/customers/${account.stripeId}/ephemeral_keys")
            .setUserId(account.id)
            .query("api_version", apiVersion)
            .noCache()
            .sendJson()
            .endJsonText()

        return body
    }

    // Deprecated
    fun loadPlan(id: String): StripePrice? {
        val (_, body) = Fetch()
            .get("${SubsApi.STRIPE_PLAN}/$id")
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
            .post(baseUrl(account.isTest) + "/subs")
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
            .post(baseUrl(account.isTest) + "/subs/$subsId/refresh")
            .setUserId(account.id)
            .noCache()
            .sendJson()
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
            .post(Endpoint.subsBase(account.isTest) + "/subs/$subsId/upgrade")
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
            .post(baseUrl(account.isTest) + "/subs/$subsId/cancel")
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
            .post(baseUrl(account.isTest) + "/subs/$subsId/reactivate")
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
