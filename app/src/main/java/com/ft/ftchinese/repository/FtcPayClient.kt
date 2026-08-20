package com.ft.ftchinese.repository

import android.os.Build
import com.ft.ftchinese.BuildConfig
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

    private fun detailedPaymentError(
        paymentMethod: String,
        endpoint: String,
        params: OrderParams,
        statusCode: Int?,
        code: String?,
        message: String?,
        exceptionName: String? = null,
    ): String {
        val detail = message?.trim()?.takeIf { it.isNotEmpty() } ?: "无错误消息"
        return listOf(
            "${paymentMethod}下单失败",
            "HTTP=${statusCode ?: "unknown"} code=${code ?: "none"}",
            "错误=$detail",
            "异常=${exceptionName ?: "APIError"}",
            "接口=$endpoint",
            "priceId=${params.priceId}",
            "discountId=${params.discountId ?: "none"}",
            "ccode=${params.ccode ?: "none"} from=${params.from ?: "none"}",
            "客户端=${BuildConfig.VERSION_NAME} Android=${Build.VERSION.SDK_INT}",
            "设备=${Build.MANUFACTURER} ${Build.MODEL}"
        ).joinToString("\n").take(1800)
    }

    fun verifyOrder(account: Account, orderId: String):  VerificationResult? {

        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setApiKey()
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
            .setLegacyApiKey()
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
        val api = ApiConfig.ofSubs(account.isTest)
        try {
            val wxOrder = withContext(Dispatchers.IO) {
                createWxOrder(account, params)
            } ?: return FetchResult.TextError(
                detailedPaymentError(
                    paymentMethod = "微信支付",
                    endpoint = api.wxOrder,
                    params = params,
                    statusCode = null,
                    code = "empty_response",
                    message = "服务器返回空响应",
                )
            )

            if (wxOrder.params.app == null) {
                return FetchResult.TextError("WxPayIntent.params.app should not be null")
            }

            return FetchResult.Success(wxOrder)
        } catch (e: APIError) {
            return if (e.statusCode == 403) {
                FetchResult.LocalizedError(R.string.duplicate_purchase)
            } else {
                FetchResult.TextError(
                    detailedPaymentError(
                        paymentMethod = "微信支付",
                        endpoint = api.wxOrder,
                        params = params,
                        statusCode = e.statusCode,
                        code = e.code,
                        message = e.message,
                    )
                )
            }
        } catch (e: Exception) {
            return FetchResult.TextError(
                detailedPaymentError(
                    paymentMethod = "微信支付",
                    endpoint = api.wxOrder,
                    params = params,
                    statusCode = null,
                    code = null,
                    message = e.message,
                    exceptionName = e::class.simpleName,
                )
            )
        }
    }

    private fun createAliOrder(account: Account, params: OrderParams): AliPayIntent? {
        val api = ApiConfig.ofSubs(account.isTest)
        return Fetch()
            .setLegacyApiKey()
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
        val api = ApiConfig.ofSubs(account.isTest)
        try {
            val aliOrder = withContext(Dispatchers.IO) {
                createAliOrder(account, params)
            } ?: return FetchResult.TextError(
                detailedPaymentError(
                    paymentMethod = "支付宝",
                    endpoint = api.aliOrder,
                    params = params,
                    statusCode = null,
                    code = "empty_response",
                    message = "服务器返回空响应",
                )
            )

            return FetchResult.Success(aliOrder)
        } catch (e: APIError) {
            return if (e.statusCode == 403) {
                FetchResult.LocalizedError(R.string.duplicate_purchase)
            } else {
                FetchResult.TextError(
                    detailedPaymentError(
                        paymentMethod = "支付宝",
                        endpoint = api.aliOrder,
                        params = params,
                        statusCode = e.statusCode,
                        code = e.code,
                        message = e.message,
                    )
                )
            }
        } catch (e: Exception) {
            return FetchResult.TextError(
                detailedPaymentError(
                    paymentMethod = "支付宝",
                    endpoint = api.aliOrder,
                    params = params,
                    statusCode = null,
                    code = null,
                    message = e.message,
                    exceptionName = e::class.simpleName,
                )
            )
        }
    }

    // Request api to add add-on to expiration date.
    fun useAddOn(account: Account): Membership? {
        val api = ApiConfig.ofSubs(account.isTest)

        return Fetch()
            .setLegacyApiKey()
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
