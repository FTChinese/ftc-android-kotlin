package com.ft.ftchinese.repository

import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.HttpResp
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.CustomerParams
import com.ft.ftchinese.model.stripesubs.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StripeClient {

    const val TAG = "StripeClient"

    // Retrieve a list of stripe prices.
    // If use is logged-in and it is a test account, use sandbox
    // api to get test prices; otherwise retrieve prices from live mode.
    fun listPrices(): HttpResp<List<StripePrice>> {
        return Fetch()
            .get(Endpoint.stripePrices)
            .noCache()
            .setApiKey()
            .endJson(withRaw = true)
    }

    fun createCustomer(account: Account): HttpResp<StripeCustomer> {
        return Fetch()
            .post(Endpoint.stripeCustomers)
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .send()
            .endJson()
    }

    fun retrieveCustomer(account: Account): HttpResp<StripeCustomer> {
        return Fetch()
            .get("${Endpoint.stripeCustomers}/${account.stripeId}")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .endJson()
    }

    fun retrievePaymentMethod(id: String): HttpResp<StripePaymentMethod> {
        return Fetch()
            .get("${Endpoint.stripePaymentMethod}/$id")
            .noCache()
            .setApiKey()
            .endJson()
    }

    fun setDefaultPaymentMethod(account: Account, pmId: String): HttpResp<StripeCustomer> {
        return Fetch()
            .post("${Endpoint.stripeCustomers}/${account.stripeId}/default-payment-method")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .sendJson(mapOf(
                "defaultPaymentMethod" to pmId
            ))
            .endJson()
    }

    @Deprecated("")
    fun createEphemeralKey(account: Account, apiVersion: String): String? {
        if (account.stripeId.isNullOrBlank()) {
            return null
        }

        return Fetch()
            .post("${Endpoint.stripeCustomers}/${account.stripeId}/ephemeral-keys")
            .setUserId(account.id)
            .addQuery("api_version", apiVersion)
            .noCache()
            .setApiKey()
            .send()
            .endText()
            .body
    }

    fun setupWithEphemeral(customerId: String): HttpResp<PaymentSheetParams> {
        return Fetch()
            .post("${Endpoint.stripePaymentSheet}/setup")
            .noCache()
            .setApiKey()
            .sendJson(CustomerParams(customer = customerId))
            .endJson()
    }

    private fun subsDefaultPaymentMethod(subsId: String, ftcId: String): HttpResp<StripePaymentMethod> {
        return Fetch()
            .get("${Endpoint.stripeSubs}/${subsId}/default-payment-method")
            .setUserId(ftcId)
            .noCache()
            .setApiKey()
            .endJson()
    }

    private fun cusDefaultPaymentMethod(cusId: String, ftcId: String): HttpResp<StripePaymentMethod> {
        return Fetch()
            .get("${Endpoint.stripeCustomers}/${cusId}/default-payment-method")
            .setUserId(ftcId)
            .noCache()
            .setApiKey()
            .endJson()
    }

    fun loadDefaultPaymentMethod(cusId: String, subsId: String?, ftcId: String): HttpResp<StripePaymentMethod> {
        if (subsId != null) {
            return subsDefaultPaymentMethod(
                subsId = subsId,
                ftcId = ftcId,
            )
        }

        return cusDefaultPaymentMethod(
            cusId = cusId,
            ftcId = ftcId,
        )
    }

    fun loadSubscription(account: Account, subsId: String): HttpResp<StripeSubs> {
        return Fetch().get("${Endpoint.stripeSubs}/${subsId}")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .endJson()
    }

    fun createSubscription(account: Account, params: SubParams): StripeSubsResult? {

        return Fetch()
            .post(Endpoint.stripeSubs)
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .sendJson(params)
            .endJson<StripeSubsResult>()
            .body
    }

    // Ask API to update user's Stripe subscription data.
    fun refreshSub(account: Account): StripeSubsResult? {

        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        return Fetch()
            .post("${Endpoint.stripeSubs}/$subsId/refresh")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .send()
            .endJson<StripeSubsResult>()
            .body
    }

    suspend fun asyncRefreshSub(account: Account): FetchResult<StripeSubsResult> {
        try {
            val stripeSub = withContext(Dispatchers.IO) {
                refreshSub(account)
            }

            return if (stripeSub == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(stripeSub)
            }
        } catch (e: APIError) {
            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.fromApi(e)
            }

        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun updateSubs(account: Account, params: SubParams): StripeSubsResult? {

        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        return Fetch()
            .post("${Endpoint.stripeSubs}/$subsId")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .sendJson(params)
            .endJson<StripeSubsResult>()
            .body
    }

    fun cancelSub(account: Account): StripeSubsResult? {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        return Fetch()
            .post("${Endpoint.stripeSubs}/$subsId/cancel")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .send()
            .endJson<StripeSubsResult>()
            .body
    }

    suspend fun asyncCancelSub(account: Account): FetchResult<StripeSubsResult> {
        try {
            val stripeSub = withContext(Dispatchers.IO) {
                cancelSub(account)
            }

            return if (stripeSub == null) {
                FetchResult.LocalizedError(R.string.stripe_refresh_failed)
            } else {
                FetchResult.Success(stripeSub)
            }
        } catch (e: APIError) {
            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.fromApi(e)
            }

        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun reactivateSub(account: Account): StripeSubsResult? {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        return Fetch()
            .post("${Endpoint.stripeSubs}/$subsId/reactivate")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .send()
            .endJson<StripeSubsResult>()
            .body
    }

    suspend fun asyncReactiveSub(account: Account): FetchResult<StripeSubsResult> {
        try {
            val stripeSub = withContext(Dispatchers.IO) {
                reactivateSub(account)
            }

            return if (stripeSub == null) {
                FetchResult.LocalizedError(R.string.stripe_refresh_failed)
            } else {
                FetchResult.Success(stripeSub)
            }
        } catch (e: APIError) {
            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.fromApi(e)
            }

        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }
}
