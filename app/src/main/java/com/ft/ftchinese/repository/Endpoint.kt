package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

object NextApi {
    private val BASE = Endpoint.readerBase
    val EMAIL_EXISTS = "$BASE/users/exists"
    val LOGIN = "$BASE/users/login"
    val SIGN_UP = "$BASE/users/signup"
    val PASSWORD_RESET = "$BASE/users/password-reset"
    val PASSWORD_RESET_LETTER = "$PASSWORD_RESET/letter"
    val VERIFY_PW_RESET = "$PASSWORD_RESET/codes"
    // Refresh account data.
    val ACCOUNT = "$BASE/user/account/v2"
    val PROFILE = "$BASE/user/profile"
    val UPDATE_EMAIL = "$BASE/user/email"
    // Resend email verification letter
    val REQUEST_VERIFICATION = "$BASE/user/email/request-verification"
    val UPDATE_USER_NAME = "$BASE/user/name"
    val UPDATE_PASSWORD = "$BASE/user/password"
    val ORDERS = "$BASE/user/orders"
    val STARRED = "$BASE/user/starred"
    val WX_ACCOUNT = "$BASE/user/wx/account/v2"
    val WX_SIGNUP = "$BASE/user/wx/signup"
    val WX_LINK = "$BASE/user/wx/link"

    val latestRelease = "$BASE/apps/android/latest"
    val releaseOf = "$BASE/apps/android/releases"
}

object ContentApi {
    private val BASE = Endpoint.contentBase

    val STORY = "$BASE/stories"
    val INTERACTIVE = "$BASE/interactive/contents"
}

private const val devIP = "http://192.168.10.107"

object Endpoint {
    val readerBase = if (BuildConfig.DEBUG) {
        "$devIP:8000"
    } else {
        BuildConfig.API_READER_LIVE
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
     */
    fun subsBase(isTest: Boolean = false) = if (isTest) {
        if (BuildConfig.DEBUG) {
            "$devIP:8200"
        } else {
            BuildConfig.API_SUBS_SANDBOX
        }
    } else {
        if (BuildConfig.DEBUG) {
            "$devIP:8200"
        } else {
            BuildConfig.API_SUBS_LIVE
        }
    }
}

object SubsApi {
    private val BASE = Endpoint.subsBase(false)

    val WX_LOGIN = "$BASE/wx/oauth/login"
    val WX_REFRESH = "$BASE/wx/oauth/refresh"

    val STRIPE_PLAN = "$BASE/stripe/plans"
    val STRIPE_CUSTOMER = "$BASE/stripe/customers"

}

const val LAUNCH_SCHEDULE_URL = "https://api003.ftmailbox.com/index.php/jsapi/applaunchschedule"
