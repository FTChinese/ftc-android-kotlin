package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

private const val devIP = "http://192.168.1.42"
private const val devPort = "8205"

object NextApi {
    private val readerBase = if (BuildConfig.DEBUG) {
        "$devIP:8000"
    } else {
        BuildConfig.API_READER_LIVE
    }

    @Deprecated("Deprecated since 4.2.2")
    val ORDERS = "$readerBase/user/orders"

    val latestRelease = "$readerBase/apps/android/latest"
    val releaseOf = "$readerBase/apps/android/releases"
}

object ContentApi {
    private val BASE = Endpoint.contentBase

    val STORY = "$BASE/stories"
    val INTERACTIVE = "$BASE/interactive/contents"
}

object Endpoint {

    val accessToken = if (BuildConfig.DEBUG) {
        BuildConfig.ACCESS_TOKEN_TEST
    } else {
        BuildConfig.ACCESS_TOKEN_LIVE
    }

    val contentBase = if (BuildConfig.DEBUG) {
        "$devIP:8100"
    } else {
        BuildConfig.API_CONTENT_LIVE
    }

    /**
     * Base url for subscription api.
     * @isTest indicates whether the current user is a test account.
     * For endpoints that do not requires user being logged in,
     * use the default value.
     *
     * Live/test modes is determined by user account settings.
     *
     * For testing account, DEBUG reaches local server or sandbox server;
     * otherwise reaches local server or live server.
     *
     * For Alipay/Wechat, use test account to get test data; otherwise live data.
     * You cannot use wechat pay in debug mode, so to test them correctly, do it
     * only in production app.
     *
     * For Apple, we also use test account to distinguish Product/Sandbox data.
     */
    fun subsBase(isTest: Boolean = false) = if (isTest) {
        // When account is test, always use sandbox url, which might be local or online
        if (BuildConfig.DEBUG) {
            "$devIP:${devPort}"
        } else {
            BuildConfig.API_SUBS_SANDBOX
        }
    } else {
        // When account is not test, always use live url, which might be local or online
        if (BuildConfig.DEBUG) {
            "$devIP:${devPort}"
        } else {
            BuildConfig.API_SUBS_LIVE
        }
    }

    fun paywall(isTest: Boolean): String {
        return "${subsBase(isTest)}/paywall"
    }

    fun refreshIAP(isTest: Boolean, origTxID: String): String {
        return "${subsBase(isTest)}/apple/subs/$origTxID"
    }

    // We cannot use production sandbox server for stripe
    // since the it is impossible to determine the url dynamically
    // for release version of the app, due to the fact that
    // the api key is initialized upon app start.
    // So release version is always using the the live key,
    // which makes it impossible to send request to production sandbox server
    // as the request will eventually denies by Stripe.
    // Thus, to test stripe, use debug version.
    private val stripeBase = "${subsBase()}/stripe"
    val stripePrices = "${stripeBase}/prices"
    val stripeCustomers = "${stripeBase}/customers"
    val stripeSubs = "${stripeBase}/subs"

    private val authEmailBase = "${subsBase()}/auth/email"
    val emailExists = "${authEmailBase}/exists"
    val emailLogin = "${authEmailBase}/login"
    val emailSignUp = "${authEmailBase}/signup"

    private val authMobileBase = "${subsBase()}/auth/mobile"
    val mobileVerificationCode = "${authMobileBase}/verification"
    val mobileInitialLink = "${authMobileBase}/link"
    val mobileSignUp = "${authMobileBase}/signup"

    val passwordReset = "${subsBase()}/auth/password-reset"
    val passwordResetLetter = "${passwordReset}/letter"
    val passwordResetCodes = "${passwordReset}/codes"

    private val authWxBase = "${subsBase()}/auth/wx"
    val wxLogin = "${authWxBase}/login"
    val wxRefresh = "${authWxBase}/refresh"

    val ftcAccount = "${subsBase()}/account"
    val email = "${ftcAccount}/email"
    val emailVrfLetter = "${ftcAccount}/email/request-verification"

    val userName = "${ftcAccount}/name"
    val passwordUpdate = "${ftcAccount}/password"
    val address = "${ftcAccount}/address"
    val wxAccount = "${ftcAccount}/wx"
    val wxSignUp = "${wxAccount}/signup"
    val wxLink = "${wxAccount}/link"
    val wxUnlink = "${wxAccount}/unlink"

    const val splashSchedule = "/index.php/jsapi/applaunchschedule"
}

