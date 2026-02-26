package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.enums.ApiMode

private const val devIP = "http://192.168.1.20"
//private const val devPort = "8206"
private const val devPort = "3000"

data class ApiConfig(
    val baseUrl: String,
    val accessToken: String,
    val mode: ApiMode,
) {
    companion object {
        private const val flavorWx = "wechat"
        private const val flavorStripe = "stripe"

        // Used for development.
        private val debugApi = ApiConfig(
            // MARK: - We don't have the server side source code, just use live server for local debug
//            baseUrl = BuildConfig.API_SUBS_LIVE,
//            accessToken = BuildConfig.ACCESS_TOKEN_LIVE,
//            mode = ApiMode.Live


            baseUrl = "$devIP:${devPort}/api",
            accessToken = BuildConfig.ACCESS_TOKEN_TEST,
            mode = ApiMode.Debug
        )

        // Production server with sandbox data
        private val releaseSandboxApi = ApiConfig(
            baseUrl = BuildConfig.API_SUBS_SANDBOX,
            accessToken = BuildConfig.ACCESS_TOKEN_LIVE,
            mode = ApiMode.Sandbox
        )

        // Production server with live data
        private val releaseLiveApi = ApiConfig(
            baseUrl = BuildConfig.API_SUBS_LIVE,
            accessToken = BuildConfig.ACCESS_TOKEN_LIVE,
            mode = ApiMode.Live
        )

        val ofAuth = if (BuildConfig.DEBUG || BuildConfig.FLAVOR == flavorWx) {
            debugApi
        } else {
            releaseLiveApi
        }

        @JvmStatic
        fun ofSubs(isTest: Boolean): ApiConfig {
            return when {
                // For Build Variants wechatRelease,
                // use local ip so that we could debug requests.
                BuildConfig.FLAVOR == flavorWx  -> {
                    debugApi
                }
                BuildConfig.FLAVOR == flavorStripe && BuildConfig.DEBUG && isTest -> {
                    releaseSandboxApi
                }
                // Debug environment always uses debug api.
                BuildConfig.DEBUG -> {
                    debugApi
                }
                // Distinguish between sandbox/live API in release app
                isTest -> {
                    releaseSandboxApi
                }
                else -> {
                    releaseLiveApi
                }
            }
        }
    }

    val paywall: String
        get() = "${baseUrl}/paywall"

    val wxOrder: String
        get() = "${baseUrl}/wxpay/app"

    val aliOrder: String
        get() = "${baseUrl}/alipay/app"

    val addOn: String
        get() = "${baseUrl}/membership/addons"

    fun verifyOrder(id: String) = "${baseUrl}/orders/$id/verify-payment"

    private val stripeBase = "${baseUrl}/stripe"
    val stripeCustomers: String
        get() = "${stripeBase}/customers"

    val stripeSubs: String
        get() = "${stripeBase}/subs"

    val stripePaymentSheet: String
        get() = "${stripeBase}/payment-sheet"

    val stripePaymentMethod
        get() = "${stripeBase}/payment-methods"

    fun refreshIAP(originalTxId: String): String {
        return "${baseUrl}/apple/subs/$originalTxId"
    }
}


