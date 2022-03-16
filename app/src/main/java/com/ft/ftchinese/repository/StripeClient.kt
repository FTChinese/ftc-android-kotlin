package com.ft.ftchinese.repository

import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.HttpResp
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeCustomer
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.model.stripesubs.SubParams

object StripeClient {

    const val TAG = "StripeClient"

    // Retrieve a list of stripe prices.
    // If use is logged-in and it is a test account, use sandbox
    // api to get test prices; otherwise retrieve prices from live mode.
    fun listPrices(): HttpResp<String> {
        return Fetch()
            .get(Endpoint.stripePrices)
            .noCache()
            .endApiText()
    }

    fun createCustomer(account: Account): HttpResp<StripeCustomer> {
        return Fetch()
            .post(Endpoint.stripeCustomers)
            .setUserId(account.id)
            .noCache()
            .sendJson()
            .endApiJson()
    }

    fun retrieveCustomer(account: Account): HttpResp<StripeCustomer> {
        return Fetch()
            .get("${Endpoint.stripeCustomers}/${account.stripeId}")
            .setUserId(account.id)
            .noCache()
            .endApiJson()
    }

    fun setDefaultPaymentMethod(account: Account, pmId: String): HttpResp<StripeCustomer> {
        return Fetch()
            .post("${Endpoint.stripeCustomers}/${account.stripeId}/default-payment-method")
            .setUserId(account.id)
            .noCache()
            .sendJson(Klaxon().toJsonString(mapOf(
                "defaultPaymentMethod" to pmId
            )))
            .endApiJson()
    }

    fun createEphemeralKey(account: Account, apiVersion: String): String? {
        if (account.stripeId == null) {
            return null
        }

        return Fetch()
            .post("${Endpoint.stripeCustomers}/${account.stripeId}/ephemeral-keys")
            .setUserId(account.id)
            .addQuery("api_version", apiVersion)
            .noCache()
            .sendJson()
            .endApiText()
            .body
    }

    fun createSubscription(account: Account, params: SubParams): StripeSubsResult? {

        return Fetch()
            .post(Endpoint.stripeSubs)
            .setUserId(account.id)
            .noCache()
            .sendJson(params.toJsonString())
            .endApiJson<StripeSubsResult>()
            .body
    }

    // Ask API to update user's Stripe subscription data.
    fun refreshSub(account: Account): StripeSubsResult? {

        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        return Fetch()
            .post("${Endpoint.stripeSubs}/$subsId/refresh")
            .setUserId(account.id)
            .noCache()
            .sendJson()
            .endApiJson<StripeSubsResult>()
            .body
    }

    fun updateSubs(account: Account, params: SubParams): StripeSubsResult? {

        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        return Fetch()
            .post("${Endpoint.stripeSubs}/$subsId")
            .setUserId(account.id)
            .noCache()
            .sendJson(json.toJsonString(params))
            .endApiJson<StripeSubsResult>()
            .body
    }

    fun cancelSub(account: Account): StripeSubsResult? {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        return Fetch()
            .post("${Endpoint.stripeSubs}/$subsId/cancel")
            .setUserId(account.id)
            .noCache()
            .sendJson()
            .endApiJson<StripeSubsResult>()
            .body
    }

    fun reactivateSub(account: Account): StripeSubsResult? {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        return Fetch()
            .post("${Endpoint.stripeSubs}/$subsId/reactivate")
            .setUserId(account.id)
            .noCache()
            .sendJson()
            .endApiJson<StripeSubsResult>()
            .body
    }
}
