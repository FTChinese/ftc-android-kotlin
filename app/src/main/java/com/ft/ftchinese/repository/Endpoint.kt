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

private const val devIP = "http://192.168.10.115"

object Endpoint {

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

    private val authWxBase = "${subsBase()}/auth"
    val wxLogin = "${authWxBase}/wx/login"
    val wxRefresh = "${authWxBase}/wx/refresh"

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
     */
    fun subsBase(isTest: Boolean = false) = if (isTest) {
        if (BuildConfig.DEBUG) {
            "$devIP:8202"
        } else {
            BuildConfig.API_SUBS_SANDBOX
        }
    } else {
        if (BuildConfig.DEBUG) {
            "$devIP:8202"
        } else {
            BuildConfig.API_SUBS_LIVE
        }
    }

    const val splashScheduleUrl = "https://api003.ftmailbox.com/index.php/jsapi/applaunchschedule"
}

