package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.JSONResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.stripesubs.SubParams
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.model.price.Price
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeCustomer
import com.ft.ftchinese.model.stripesubs.StripeSetupIntent
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object StripeClient : AnkoLogger {

    // For stripe pay, we do not distinguish test account.
    private val baseUrl = Endpoint.subsBase(BuildConfig.DEBUG) + "/stripe"

    // Retrieve a list of stripe prices.
    // If use is logged-in and it is a test account, use sandbox
    // api to get test prices; otherwise retrieve prices from live mode.
    fun listPrices(): JSONResult<List<Price>>? {

        val (_, body) = Fetch()
            .get("$baseUrl/prices")
            .noCache()
            .endJsonText()

        if (body == null) {
            return null
        }

        val prices = json.parseArray<Price>(body)
        return if (prices == null) {
            null
        } else {
            JSONResult(prices, body)
        }
    }

    fun createCustomer(account: Account): JSONResult<StripeCustomer>? {
        val (_, body) = Fetch()
            .post("$baseUrl/customers")
            .setUserId(account.id)
            .noCache()
            .sendJson()
            .endJsonText()

        if (body == null) {
            return null
        }

        val c = json.parse<StripeCustomer>(body)
        return if (c == null) {
            null
        } else {
            JSONResult(c, body)
        }
    }

    fun retrieveCustomer(account: Account): JSONResult<StripeCustomer>? {
        val (_, body) = Fetch()
            .get("$baseUrl/customers/${account.stripeId}")
            .setUserId(account.id)
            .noCache()
            .endJsonText()

        if (body == null) {
            return null
        }

        val c = json.parse<StripeCustomer>(body)
        return if (c == null) {
            null
        } else {
            JSONResult(c, body)
        }
    }

    fun setDefaultPaymentMethod(account: Account, pmId: String): JSONResult<StripeCustomer>? {
        val (_, body) = Fetch()
            .post("$baseUrl/customers/${account.stripeId}/default-payment-method")
            .setUserId(account.id)
            .noCache()
            .sendJson(Klaxon().toJsonString(mapOf(
                "defaultPaymentMethod" to pmId
            )))
            .endJsonText()

        if (body == null) {
            return null
        }

        val c = json.parse<StripeCustomer>(body)
        return if (c == null) {
            null
        } else {
            JSONResult(c, body)
        }
    }

    fun createEphemeralKey(account: Account, apiVersion: String): String? {
        if (account.stripeId == null) {
            return null
        }

        val (_, body) = Fetch()
            .post("$baseUrl/customers/${account.stripeId}/ephemeral-keys")
            .setUserId(account.id)
            .query("api_version", apiVersion)
            .noCache()
            .sendJson()
            .endJsonText()

        return body
    }

    fun createSubscription(account: Account, params: SubParams): StripeSubsResult? {

        val (_, body ) = Fetch()
            .post("$baseUrl/subs")
            .setUserId(account.id)
            .noCache()
            .sendJson(json.toJsonString(params))
            .endJsonText()

        return if (body == null) {
            null
        } else {
            json.parse<StripeSubsResult>(body)
        }
    }

    // Ask API to update user's Stripe subscription data.
    fun refreshSub(account: Account): StripeSubsResult? {

        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val (_, body) = Fetch()
            .post("$baseUrl/subs/$subsId/refresh")
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

    fun updateSubs(account: Account, params: SubParams): StripeSubsResult? {

        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val (_, body) = Fetch()
            .post("$baseUrl/subs/$subsId")
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

    fun cancelSub(account: Account): StripeSubsResult? {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val (_, body) = Fetch()
            .post("$baseUrl/subs/$subsId/cancel")
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

    fun reactivateSub(account: Account): StripeSubsResult? {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val (_, body) = Fetch()
            .post("$baseUrl/subs/$subsId/reactivate")
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
