package com.ft.ftchinese.repository

import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.VerificationResult
import com.ft.ftchinese.model.ftcsubs.WxPayIntent
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.request.OrderParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FtcPayClient {

    fun verifyOrder(account: Account, orderId: String):  VerificationResult? {

        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setBearer(api.accessToken)
            .post(api.verifyOrder(orderId))
            .addHeaders(account.headers())
            .noCache()
            .send()
            .endJson<VerificationResult>()
            .body
    }

    private fun createWxOrder(account: Account, params: OrderParams): WxPayIntent? {

        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setBearer(api.accessToken)
            .post(api.wxOrder)
            .addHeaders(account.headers())
            .setTimeout(30)
            .noCache()
            .setClient()
            .sendJson(params)
            .endJson<WxPayIntent>()
            .body
    }

    suspend fun asyncCreateWxOrder(account: Account, params: OrderParams): FetchResult<WxPayIntent> {
        try {
            val wxOrder = withContext(Dispatchers.IO) {
                createWxOrder(account, params)
            } ?: return FetchResult.LocalizedError(R.string.toast_order_failed)

            if (wxOrder.params.app == null) {
                return FetchResult.TextError("WxPayIntent.params.app should not be null")
            }

            return FetchResult.Success(wxOrder)
        } catch (e: APIError) {
            return if (e.statusCode == 403) {
                FetchResult.LocalizedError(R.string.duplicate_purchase)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    private fun createAliOrder(account: Account, params: OrderParams): AliPayIntent? {
        val api = ApiConfig.ofSubs(account.isTest)
        return Fetch()
            .setBearer(api.accessToken)
            .post(api.aliOrder)
            .setTimeout(30)
            .addHeaders(account.headers())
            .noCache()
            .setClient()
            .sendJson(params)
            .endJson<AliPayIntent>()
            .body
    }

    suspend fun asyncCreateAliOrder(account: Account, params: OrderParams): FetchResult<AliPayIntent> {
        try {
            val aliOrder = withContext(Dispatchers.IO) {
                createAliOrder(account, params)
            } ?: return FetchResult.LocalizedError(R.string.toast_order_failed)

            return FetchResult.Success(aliOrder)
        } catch (e: APIError) {
            return if (e.statusCode == 403) {
                FetchResult.LocalizedError(R.string.duplicate_purchase)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }

    // Request api to add add-on to expiration date.
    fun useAddOn(account: Account): Membership? {
        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setBearer(api.accessToken)
           .post(api.addOn)
           .addHeaders(account.headers())
           .noCache()
           .send()
           .endJson<Membership>()
           .body
    }

    suspend fun asyncUseAddOn(account: Account): FetchResult<Membership> {
        try {
            val m = withContext(Dispatchers.IO) {
                useAddOn(account)
            }

            return if (m == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(m)
            }
        } catch (e: APIError) {
            return  if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }
}
