package com.ft.ftchinese.util

object NextApi {
//    private const val BASE = "http://api.ftchinese.com/v1"
    private const val BASE = "http://192.168.10.195:8000"
    const val EMAIL_EXISTS = "$BASE/users/exists"
    const val AUTH = "$BASE/users/auth"
    const val LOGIN = "$BASE/users/login"
    const val NEW_ACCOUNT = "$BASE/users/new"
    const val SIGN_UP = "$BASE/users/signup"
    const val PASSWORD_RESET = "$BASE/users/password-reset/letter"
    // Refresh account data.
    const val ACCOUNT = "$BASE/user/account"
    const val PROFILE = "$BASE/user/profile"
    const val UPDATE_EMAIL = "$BASE/user/email"
    // Resend email verification letter
    const val REQUEST_VERIFICATION = "$BASE/user/email/request-verification"
    const val UPDATE_USER_NAME = "$BASE/user/name"
    const val UPDATE_PASSWORD = "$BASE/user/password"
    const val STARRED = "$BASE/user/starred"
    const val WX_ACCOUNT = "$BASE/wx/account"
    const val WX_SIGNUP = "$BASE/wx/signup"
    const val WX_BIND = "$BASE/wx/bind"
}

object SubscribeApi {
//    private const val BASE = "http://www.ftacademy.cn/api/v1"
//        private const val BASE = "http://www.ftacademy.cn/api/sandbox"
    private const val BASE = "http://192.168.10.195:8200"
    const val WX_UNIFIED_ORDER = "$BASE/wxpay/unified-order"
    const val WX_ORDER_QUERY = "$BASE/wxpay/query"
    const val ALI_ORDER = "$BASE/alipay/app-order"
    const val ALI_VERIFY_APP_PAY = "$BASE/alipay/verify/app-pay"

    const val WX_LOGIN = "$BASE/wx/oauth/login"
    const val WX_REFRESH = "$BASE/wx/oauth/refresh"
}