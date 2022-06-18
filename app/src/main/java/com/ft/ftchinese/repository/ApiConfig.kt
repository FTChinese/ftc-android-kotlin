package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

private const val devIP = "http://192.168.0.4"
private const val devPort = "8206"

enum class ApiMode {
    Debug,
    Sandbox,
    Live,
}

data class ApiConfig(
    val baseUrl: String,
    val accessToken: String,
    val mode: ApiMode,
) {
    companion object {
        private const val flavorWx = "wechat"

        // Used for development.
        private val debugApi = ApiConfig(
            baseUrl = "$devIP:${devPort}",
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
                // For other Build Variants xxxDebug
                BuildConfig.DEBUG -> {
                    debugApi
                }
                // For Build Variants xxxRelease.
                // Sandbox is dynamically determined by user's account.
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


