package com.ft.ftchinese.repository

import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.HttpResp
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.CustomerParams
import com.ft.ftchinese.model.request.PaymentMethodParams
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

    private fun createCustomer(account: Account): HttpResp<StripeCustomer> {
        return Fetch()
            .post(Endpoint.stripeCustomers)
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .send()
            .endJson()
    }

    suspend fun asyncCreateCustomer(account: Account): FetchResult<StripeCustomer> {
        try {
            val resp = withContext(Dispatchers.IO) {
                createCustomer(account)
            }

            return if (resp.body == null) {
                FetchResult.LocalizedError(R.string.stripe_customer_not_created)
            } else {
                FetchResult.Success(resp.body)
            }
        } catch (e: APIError) {
            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.stripe_customer_not_found)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    fun retrieveCustomer(account: Account): HttpResp<StripeCustomer> {
        return Fetch()
            .get("${Endpoint.stripeCustomers}/${account.stripeId}")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .endJson()
    }

    private fun retrievePaymentMethod(id: String, refresh: Boolean): HttpResp<StripePaymentMethod> {
        return Fetch()
            .get("${Endpoint.stripePaymentMethod}/$id")
            .addQuery("refresh", "$refresh")
            .noCache()
            .setApiKey()
            .endJson()
    }

    suspend fun asyncRetrievePaymentMethod(id: String, refresh: Boolean): FetchResult<StripePaymentMethod> {
        return try {
            val resp = withContext(Dispatchers.IO) {
                retrievePaymentMethod(id, refresh)
            }

            if (resp.body == null) {
                FetchResult.unknownError
            } else {
                FetchResult.Success(resp.body)
            }
        } catch (e: APIError) {
            FetchResult.fromApi(e)
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    // Set default payment method under customer.
    private fun setCusDefaultPayment(account: Account, pmId: String): HttpResp<StripeCustomer> {
        return Fetch()
            .post("${Endpoint.stripeCustomers}/${account.stripeId}/default-payment-method")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .sendJson(
                PaymentMethodParams(
                    defaultPaymentMethod = pmId
                )
            )
            .endJson()
    }

    suspend fun asyncSetCusDefaultPayment(account: Account, paymentMethodId: String): FetchResult<StripeCustomer> {
        try {
            val resp = withContext(Dispatchers.IO) {
                setCusDefaultPayment(account, paymentMethodId)
            }

            return if (resp.body == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(resp.body)
            }
        } catch (e: APIError) {
            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.stripe_customer_not_found)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    private fun setSubsDefaultPayment(account: Account, paymentMethodId: String): HttpResp<StripeSubs> {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")
        return Fetch()
            .post("${Endpoint.stripeSubs}/$subsId/default-payment-method")
            .setUserId(account.id)
            .noCache()
            .setApiKey()
            .sendJson(PaymentMethodParams(
                defaultPaymentMethod = paymentMethodId
            ))
            .endJson()
    }

    suspend fun asyncSetSubsDefaultPayment(account: Account, paymentMethodId: String): FetchResult<StripeSubs> {
        try {
            val resp = withContext(Dispatchers.IO) {
                setSubsDefaultPayment(account, paymentMethodId)
            }

            return if (resp.body == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(resp.body)
            }
        } catch (e: APIError) {
            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.stripe_customer_not_found)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
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

    private fun setupWithEphemeral(customerId: String): HttpResp<PaymentSheetParams> {
        return Fetch()
            .post("${Endpoint.stripePaymentSheet}/setup")
            .noCache()
            .setApiKey()
            .sendJson(CustomerParams(customer = customerId))
            .endJson()
    }

    suspend fun asyncSetupWithEphemeral(customerId: String): FetchResult<PaymentSheetParams> {
        return try {
            val resp = withContext(Dispatchers.IO) {
                setupWithEphemeral(customerId)
            }

            if (resp.body == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(resp.body)
            }
        } catch (e: APIError) {
            FetchResult.fromApi(e)
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
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

    private fun loadDefaultPaymentMethod(cusId: String, subsId: String?, ftcId: String): HttpResp<StripePaymentMethod> {
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

    suspend fun asyncLoadDefaultPaymentMethod(
        cusId: String,
        subsId: String?,
        ftcId: String,
    ): FetchResult<StripePaymentMethod> {
        try {
            val resp = withContext(Dispatchers.IO) {
                loadDefaultPaymentMethod(
                    cusId = cusId,
                    subsId = subsId,
                    ftcId = ftcId
                )
            }

            return if (resp.body == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(resp.body)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
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

    suspend fun asyncCreateSubs(account: Account, params: SubParams): FetchResult<StripeSubsResult> {
        return try {
            val result = withContext(Dispatchers.IO) {
                createSubscription(account, params)
            }

            if (result == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(result)
            }
        } catch (e: APIError) {
            FetchResult.fromApi(e)
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
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

    suspend fun asyncUpdateSubs(account: Account, params: SubParams): FetchResult<StripeSubsResult> {
        return try {
            val result = withContext(Dispatchers.IO) {
                StripeClient.updateSubs(account, params)
            }

            if (result == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(result)
            }
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    private fun cancelSub(account: Account): StripeSubsResult? {
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
