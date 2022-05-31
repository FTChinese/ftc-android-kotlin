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
        return Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/orders/$orderId/verify-payment")
            .addHeaders(account.headers())
            .noCache()
            .setApiKey()
            .send()
            .endJson<VerificationResult>()
            .body
    }

    fun createWxOrder(account: Account, params: OrderParams): WxPayIntent? {

        return Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/wxpay/app")
            .addHeaders(account.headers())
            .setTimeout(30)
            .noCache()
            .setClient()
            .setApiKey()
            .setAppId() // Deprecated
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

    fun createAliOrder(account: Account, params: OrderParams): AliPayIntent? {

        return Fetch()
            .post(Endpoint.subsBase(account.isTest) + "/alipay/app")
            .setTimeout(30)
            .addHeaders(account.headers())
            .noCache()
            .setApiKey()
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
       return Fetch()
           .post(Endpoint.subsBase(account.isTest) + "/membership/addons")
           .addHeaders(account.headers())
           .noCache()
           .setApiKey()
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
