package com.ft.ftchinese.util

import com.ft.ftchinese.BuildConfig

object NextApi {
    private const val BASE = "http://api.ftchinese.com/v1"
//    private const val BASE = "http://192.168.10.195:8000"
    const val EMAIL_EXISTS = "$BASE/users/exists"
    const val LOGIN = "$BASE/users/login"
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
    const val ORDERS = "$BASE/user/orders"
    const val STARRED = "$BASE/user/starred"
    const val WX_ACCOUNT = "$BASE/wx/account"
    const val WX_SIGNUP = "$BASE/wx/signup"
    const val WX_BIND = "$BASE/wx/bind"
}

object SubscribeApi {
    private val BASE = if (BuildConfig.DEBUG) {
        "http://www.ftacademy.cn/api/sandbox"
    } else {
        "http://www.ftacademy.cn/api/v1"
    }
//    private const val BASE =
//    private const val BASE = "http://192.168.10.195:8200"
    val WX_UNIFIED_ORDER = "$BASE/wxpay/app"
    val WX_ORDER_QUERY = "$BASE/wxpay/query"
    val ALI_ORDER = "$BASE/alipay/app"

    val WX_LOGIN = "$BASE/wx/oauth/login"
    val WX_REFRESH = "$BASE/wx/oauth/refresh"
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
        "samsung" to "an_samsung",
        "meizu" to "an_meizu",
        "wandoujia" to "an_wandoujia",
        "anzhi" to "an_anzhi",
        "jiuyi" to "an_91",
        "anzhuoshichang" to "an_anzhuoshichang"
)