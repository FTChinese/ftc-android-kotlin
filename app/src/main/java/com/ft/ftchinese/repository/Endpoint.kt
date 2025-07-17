package com.ft.ftchinese.repository

import com.ft.ftchinese.BuildConfig

object Endpoint {


    val conversionTracking = if (BuildConfig.DEBUG) {
        "https://www.chineseft.net/index.php/jsapi/deeplinkcampaigntestdata"
    } else {
        "https://www.googleadservices.com/pagead/conversion/app/1.0"
    }

    const val ftcCampaign = "https://www.chineseft.net/index.php/jsapi/deeplinkcampaign"
    const val splashSchedule = "/index.php/jsapi/applaunchschedule"

    val accessToken = ApiConfig.ofAuth.accessToken
    private val baseUrl = ApiConfig.ofAuth.baseUrl

    val latestRelease = "${baseUrl}/apps/android/latest"

    private val authEmailBase = "${baseUrl}/auth/email"
    val emailExists = "${authEmailBase}/exists"
    val emailLogin = "${authEmailBase}/login"
    val emailSignUp = "${authEmailBase}/signup"

    private val authMobileBase = "${baseUrl}/auth/mobile"
    val mobileVerificationCode = "${authMobileBase}/verification"
    val mobileInitialLink = "${authMobileBase}/link"
    val mobileSignUp = "${authMobileBase}/signup"

    val passwordReset = "${baseUrl}/auth/password-reset"
    val passwordResetLetter = "${passwordReset}/letter"
    val passwordResetCodes = "${passwordReset}/codes"

    private val authWxBase = "${baseUrl}/auth/wx"
    val wxLogin = "${authWxBase}/login"
    val wxRefresh = "${authWxBase}/refresh"

    val ftcAccount = "${baseUrl}/account"
    val email = "${ftcAccount}/email"
    val emailVrfLetter = "${ftcAccount}/email/request-verification"
    val smsCode = "${ftcAccount}/mobile/verification"
    val updateMobile = "${ftcAccount}/mobile"

    val userName = "${ftcAccount}/name"
    val passwordUpdate = "${ftcAccount}/password"
    val address = "${ftcAccount}/address"
    val wxAccount = "${ftcAccount}/wx"
    val wxSignUp = "${wxAccount}/signup"
    val wxLink = "${wxAccount}/link"
    val wxUnlink = "${wxAccount}/unlink"
}

