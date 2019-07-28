package com.ft.ftchinese.util

import com.ft.ftchinese.BuildConfig

object NextApi {
    private val BASE = if (BuildConfig.DEBUG) {
        "http://192.168.10.195:8000"
    } else {
        "http://api.ftchinese.com/v1"
    }
    val EMAIL_EXISTS = "$BASE/users/exists"
    val LOGIN = "$BASE/users/login"
    val SIGN_UP = "$BASE/users/signup"
    val PASSWORD_RESET = "$BASE/users/password-reset/letter"
    // Refresh account data.
    val ACCOUNT = "$BASE/user/account/v2"
    val PROFILE = "$BASE/user/profile"
    val UPDATE_EMAIL = "$BASE/user/email"
    // Resend email verification letter
    val REQUEST_VERIFICATION = "$BASE/user/email/request-verification"
    val UPDATE_USER_NAME = "$BASE/user/name"
    val UPDATE_PASSWORD = "$BASE/user/password"
    val ORDERS = "$BASE/user/orders"
    val UNLINK = "$BASE/user/unlink/wx"
    val STARRED = "$BASE/user/starred"
    val WX_ACCOUNT = "$BASE/user/wx/account/v2"
    val WX_SIGNUP = "$BASE/user/wx/signup"
    val WX_LINK = "$BASE/user/wx/link"
}

object SubscribeApi {
    private val BASE = if (BuildConfig.DEBUG) {
//        "http://www.ftacademy.cn/api/sandbox"
        "http://192.168.10.195:8200"
    } else {
        "http://www.ftacademy.cn/api/v1"
    }
    val WX_UNIFIED_ORDER = "$BASE/wxpay/app"
    val WX_ORDER_QUERY = "$BASE/wxpay/query"
    val ALI_ORDER = "$BASE/alipay/app"



    val WX_LOGIN = "$BASE/wx/oauth/login"
    val WX_REFRESH = "$BASE/wx/oauth/refresh"

    val UPGRADE = "$BASE/upgrade/free"
    val UPGRADE_PREVIEW = "$BASE/upgrade/balance"

    val STRIPE_PLAN = "$BASE/stripe/plans"
    val STRIPE_ORDER = "$BASE/stripe/order"
    val STRIPE_CUSTOMER = "$BASE/stripe/customers"
    val STRIPE_SUB = "$BASE/stripe/subscriptions"
    val STRIPE_PAY_INTENT = "$BASE/stripe/payment_intents"
}

const val LAUNCH_SCHEDULE_URL = "https://api003.ftmailbox.com/index.php/jsapi/applaunchschedule"

const val FTC_OFFICIAL_URL = "http://www.ftchinese.com"

const val MAILBOX_URL = "https://api003.ftmailbox.com"

const val HOST_FTC = "www.ftchinese.com"
const val HOST_MAILBOX = "api003.ftmailbox.com"
const val HOST_FTA = "www.ftacademy.cn"

val flavorQuery = mapOf(
        "play" to "play_store",
        "xiaomi" to "an_xiaomi",
        "huawei" to "an_huawei",
        "baidu" to "an_baidu",
        "sanliuling" to "an_360shouji",
        "ftc" to "an_ftc",
        "tencent" to "an_tencent",
        "samsung" to "an_samsung"
//        "meizu" to "an_meizu",
//        "wandoujia" to "an_wandoujia",
//        "anzhi" to "an_anzhi",
//        "jiuyi" to "an_91",
//        "anzhuoshichang" to "an_anzhuoshichang"
)
