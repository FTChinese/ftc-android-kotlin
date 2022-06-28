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

    private fun createCustomer(account: Account): HttpResp<StripeCustomer> {
        val api = ApiConfig.ofSubs(account.isTest)
        return Fetch()
            .setBearer(api.accessToken)
            .post(api.stripeCustomers)
            .setUserId(account.id)
            .noCache()
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
        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setBearer(api.accessToken)
            .get("${api.stripeCustomers}/${account.stripeId}")
            .setUserId(account.id)
            .noCache()
            .endJson()
    }

    private fun retrievePaymentMethod(
        isTest: Boolean,
        id: String,
        refresh: Boolean
    ): HttpResp<StripePaymentMethod> {
        val api = ApiConfig.ofSubs(isTest)

        return Fetch()
            .setBearer(api.accessToken)
            .get("${api.stripePaymentMethod}/$id")
            .addQuery("refresh", "$refresh")
            .noCache()
            .endJson()
    }

    suspend fun asyncRetrievePaymentMethod(
        isTest: Boolean,
        id: String,
        refresh: Boolean
    ): FetchResult<StripePaymentMethod> {
        return try {
            val resp = withContext(Dispatchers.IO) {
                retrievePaymentMethod(
                    isTest,
                    id,
                    refresh
                )
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
        val api = ApiConfig.ofSubs(account.isTest)
        return Fetch()
            .setBearer(api.accessToken)
            .post("${api.stripeCustomers}/${account.stripeId}/default-payment-method")
            .setUserId(account.id)
            .noCache()
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

        val api = ApiConfig.ofSubs(account.isTest)
        return Fetch()
            .setBearer(api.accessToken)
            .post("${api.stripeSubs}/$subsId/default-payment-method")
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

        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setBearer(api.accessToken)
            .post("${api.stripeCustomers}/${account.stripeId}/ephemeral-keys")
            .setUserId(account.id)
            .addQuery("api_version", apiVersion)
            .noCache()
            .send()
            .endText()
            .body
    }

    private fun setupWithEphemeral(isTest: Boolean, customerId: String): HttpResp<PaymentSheetParams> {
        val api = ApiConfig.ofSubs(isTest)
        return Fetch()
            .setBearer(api.accessToken)
            .post("${api.stripePaymentSheet}/setup")
            .noCache()
            .sendJson(CustomerParams(customer = customerId))
            .endJson()
    }

    suspend fun asyncSetupWithEphemeral(isTest: Boolean, customerId: String): FetchResult<PaymentSheetParams> {
        return try {
            val resp = withContext(Dispatchers.IO) {
                setupWithEphemeral(isTest, customerId)
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

    private fun subsDefaultPaymentMethod(
        api: ApiConfig,
        subsId: String,
        ftcId: String
    ): HttpResp<StripePaymentMethod> {
        return Fetch()
            .setBearer(api.accessToken)
            .get("${api.stripeSubs}/${subsId}/default-payment-method")
            .setUserId(ftcId)
            .noCache()
            .endJson()
    }

    private fun cusDefaultPaymentMethod(
        api: ApiConfig,
        cusId: String,
        ftcId: String
    ): HttpResp<StripePaymentMethod> {

        return Fetch()
            .setBearer(api.accessToken)
            .get("${api.stripeCustomers}/${cusId}/default-payment-method")
            .setUserId(ftcId)
            .noCache()
            .endJson()
    }

    private fun loadDefaultPaymentMethod(
        account: Account
    ): HttpResp<StripePaymentMethod> {
        val api = ApiConfig.ofSubs(account.isTest)

        if (account.membership.stripeSubsId != null) {
            return subsDefaultPaymentMethod(
                api,
                subsId = account.membership.stripeSubsId,
                ftcId = account.id,
            )
        }

        account.stripeId ?: throw Exception("Not a stripe customer")

        return cusDefaultPaymentMethod(
            api,
            cusId = account.stripeId,
            ftcId = account.id,
        )
    }

    suspend fun asyncLoadDefaultPaymentMethod(
        account: Account
    ): FetchResult<StripePaymentMethod> {
        return try {
            val resp = withContext(Dispatchers.IO) {
                loadDefaultPaymentMethod(
                    account
                )
            }

            if (resp.body == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(resp.body)
            }
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }

    fun loadSubscription(account: Account, subsId: String): HttpResp<StripeSubs> {
        val api = ApiConfig.ofSubs(account.isTest)
        return Fetch()
            .setBearer(api.accessToken)
            .get("${api.stripeSubs}/${subsId}")
            .setUserId(account.id)
            .noCache()
            .endJson()
    }

    fun createSubscription(account: Account, params: SubParams): StripeSubsResult? {

        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setBearer(api.accessToken)
            .post(api.stripeSubs)
            .setUserId(account.id)
            .noCache()
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
        val api = ApiConfig.ofSubs(account.isTest)
        return Fetch()
            .setBearer(api.accessToken)
            .post("${api.stripeSubs}/$subsId/refresh")
            .setUserId(account.id)
            .noCache()
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

    private fun updateSubs(account: Account, params: SubParams): StripeSubsResult? {

        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setBearer(api.accessToken)
            .post("${api.stripeSubs}/$subsId")
            .setUserId(account.id)
            .noCache()
            .sendJson(params)
            .endJson<StripeSubsResult>()
            .body
    }

    suspend fun asyncUpdateSubs(account: Account, params: SubParams): FetchResult<StripeSubsResult> {
        return try {
            val result = withContext(Dispatchers.IO) {
                updateSubs(account, params)
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

        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setBearer(api.accessToken)
            .post("${api.stripeSubs}/$subsId/cancel")
            .setUserId(account.id)
            .noCache()
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

    private fun reactivateSub(account: Account): StripeSubsResult? {
        val subsId = account.membership.stripeSubsId ?: throw Exception("Not a stripe subscription")

        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .post("${api.stripeSubs}/$subsId/reactivate")
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
