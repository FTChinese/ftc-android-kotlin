package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

private const val devIP = "http://192.168.0.3"
private const val devPort = "8206"

data class ApiConfig(
    val baseUrl: String,
    val accessToken: String,
) {
    companion object {
        private const val flavorWx = "wechat"
        private const val flavorStripe = "stripe"

        // Used for development.
        private val debugApi = ApiConfig(
            baseUrl = "$devIP:${devPort}",
            accessToken = BuildConfig.ACCESS_TOKEN_TEST
        )

        // Production server with sandbox data
        private val releaseSandboxApi = ApiConfig(
            baseUrl = BuildConfig.API_SUBS_SANDBOX,
            accessToken = BuildConfig.ACCESS_TOKEN_LIVE
        )

        // Production server with live data
        private val releaseLiveApi = ApiConfig(
            baseUrl = BuildConfig.API_SUBS_LIVE,
            accessToken = BuildConfig.ACCESS_TOKEN_LIVE
        )

        val ofAuth = if (BuildConfig.DEBUG || BuildConfig.FLAVOR == flavorWx) {
            debugApi
        } else {
            releaseLiveApi
        }

        @JvmStatic
        fun ofSubs(isTest: Boolean): ApiConfig {
            return when {
                // For Build Variants wechatRelease
                BuildConfig.FLAVOR == flavorWx  -> {
                    debugApi
                }
                // For other Build Variants xxxDebug
                BuildConfig.DEBUG -> {
                    // For Build Variants stripeDebug. Test account-only
                    if (isTest && BuildConfig.FLAVOR == flavorStripe) {
                        releaseSandboxApi
                    } else {
                        debugApi
                    }
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

