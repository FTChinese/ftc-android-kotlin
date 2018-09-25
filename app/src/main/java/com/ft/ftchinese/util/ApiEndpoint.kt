package com.ft.ftchinese.util

class ApiEndpoint {
    companion object {
        private const val BASE = "https://lacerta.serveo.net"
        const val LOGIN = "$BASE/users/auth"
        const val NEW_ACCOUNT = "$BASE/users/new"
        const val PASSWORD_RESET = "$BASE/users/password-reset/letter"
        const val ACCOUNT = "$BASE/user/account"
        const val PROFILE = "$BASE/user/profile"
        const val UPDATE_EMAIL = "$BASE/user/email"
        const val REQUEST_VERIFICATION = "$BASE/user/email/request-verification"
        const val UPDATE_USER_NAME = "$BASE/user/name"
        const val UPDATE_PASSWORD = "$BASE/user/password"
        const val STARRED = "$BASE/user/starred"
        const val APP_LAUNCH = "https://api003.ftmailbox.com/index.php/jsapi/applaunchschedule"
    }
}

class SubscribeApi {
    companion object {
        private const val BASE = "http://www.ftacademy.cn/api/v1"
        const val WX_PREPAY_ORDER = "$BASE/order/tenpay"
        const val ALIPAY_ORDER = "$BASE/order/alipay"
    }
}