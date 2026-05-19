package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.enums.ApiMode

private const val useLocalBackend = true

// Flip useLocalBackend only when the Node backend is running locally.
private const val localBackendHost = "http://192.168.1.84"
private const val localBackendPort = "3000"

data class ApiConfig(
    val baseUrl: String,
    val accessToken: String,
    val mode: ApiMode,
) {
    companion object {
        private val shouldUseLocalBackend = useLocalBackend && BuildConfig.BUILD_TYPE == "debug"

        private val localBackendApi = ApiConfig(
            baseUrl = "$localBackendHost:${localBackendPort}/api",
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

        val ofAuth = if (shouldUseLocalBackend) {
            localBackendApi
        } else {
            releaseLiveApi
        }

        @JvmStatic
        fun ofSubs(isTest: Boolean): ApiConfig {
            return when {
                shouldUseLocalBackend -> {
                    localBackendApi
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

    val subscriptionCatalog: String
        get() = "${baseUrl}/subscription/catalog"

    val subscriptionSummary: String
        get() = "${baseUrl}/subscription/summary"

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

    fun stripeCustomerPaymentMethods(customerId: String): String {
        return "${stripeCustomers}/$customerId/payment-methods"
    }

    fun refreshIAP(originalTxId: String): String {
        return "${baseUrl}/apple/subs/$originalTxId"
    }
}
