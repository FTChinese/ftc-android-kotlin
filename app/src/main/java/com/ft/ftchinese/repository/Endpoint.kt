package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

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

private const val devIP = "http://192.168.1.26"
object Endpoint {

    val accessToken = if (BuildConfig.DEBUG) {
        BuildConfig.ACCESS_TOKEN_TEST
//        BuildConfig.ACCESS_TOKEN_LIVE
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
            "$devIP:8203"
//        BuildConfig.API_SUBS_SANDBOX
        } else {
            BuildConfig.API_SUBS_SANDBOX
        }
    } else {
        // When account is not test, always use live url, which might be local or online
        if (BuildConfig.DEBUG) {
            "$devIP:8203"
//        BuildConfig.API_SUBS_LIVE
        } else {
            BuildConfig.API_SUBS_LIVE
        }
    }

    /**
     * For Stripe, however, things are quite different since a live/test key is involved
     * in client app. For DEBUG app we are using testing key and production key otherwise.
     * This means the account type does not matter. The mode is always determined by app's
     * DEBUG configuration. Test account is meaningless here.
     * In such case simply pass DEBUG as the isTest
     * parameter.
     */
    private val apiBase = if (BuildConfig.DEBUG) {
        "$devIP:8203"
//        BuildConfig.API_SUBS_SANDBOX
    } else {
        BuildConfig.API_SUBS_LIVE
    }

    private val stripeBase = "${apiBase}/stripe"
    val stripePrices = "${stripeBase}/prices"
    val stripeCustomers = "${stripeBase}/customers"
    val stripeSubs = "${stripeBase}/subs"

    private val paywallBase = "${subsBase()}/paywall"

    fun paywall(isTest: Boolean): String {
        return "$paywallBase?live=${!isTest}"
    }

    fun refreshIAP(isTest: Boolean, origTxID: String): String {
        return "${subsBase(isTest)}/apple/subs/$origTxID"
    }

    private val authEmailBase = "$apiBase/auth/email"
    val emailExists = "${authEmailBase}/exists"
    val emailLogin = "${authEmailBase}/login"
    val emailSignUp = "${authEmailBase}/signup"

    private val authMobileBase = "$apiBase/auth/mobile"
    val mobileVerificationCode = "${authMobileBase}/verification"
    val mobileInitialLink = "${authMobileBase}/link"
    val mobileSignUp = "${authMobileBase}/signup"

    val passwordReset = "$apiBase/auth/password-reset"
    val passwordResetLetter = "${passwordReset}/letter"
    val passwordResetCodes = "${passwordReset}/codes"

    private val authWxBase = "$apiBase/auth"
    val wxLogin = "${authWxBase}/wx/login"
    val wxRefresh = "${authWxBase}/wx/refresh"

    val ftcAccount = "$apiBase/account"
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

