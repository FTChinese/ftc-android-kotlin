package com.ft.ftchinese.repository

import android.net.Uri
import com.ft.ftchinese.BuildConfig

object NextApi {
    private val BASE = if (BuildConfig.DEBUG) {
        "http://192.168.10.195:8000"
//        "http://user.ftmailbox.com/v1"
    } else {
        "http://user.ftchinese.org/v1"
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
    val STARRED = "$BASE/user/starred"
    val WX_ACCOUNT = "$BASE/user/wx/account/v2"
    val WX_SIGNUP = "$BASE/user/wx/signup"
    val WX_LINK = "$BASE/user/wx/link"

    val latestRelease = "$BASE/apps/android/latest"
    val releaseOf = "$BASE/apps/android/releases"
}

object ContentApi {
    private val BASE = if (BuildConfig.DEBUG) {
        "http://192.168.10.195:8100"
    } else {
        "http://api-content.ftchinese.com/v1"
    }

    val STORY = "$BASE/stories"
    val INTERACTIVE = "$BASE/interactive/contents"
}

object SubscribeApi {
    private val BASE = if (BuildConfig.DEBUG) {
//        "http://www.ftacademy.cn/api/sandbox"
        "http://192.168.10.195:8200"
    } else {
        "https://www.ftacademy.cn/api/v1"
    }
    val WX_UNIFIED_ORDER = "$BASE/wxpay/app"
    val WX_ORDER_QUERY = "$BASE/wxpay/query"
    val ALI_ORDER = "$BASE/alipay/app"

    val WX_LOGIN = "$BASE/wx/oauth/login"
    val WX_REFRESH = "$BASE/wx/oauth/refresh"

    val UPGRADE = "$BASE/upgrade/free"
    val UPGRADE_PREVIEW = "$BASE/upgrade/balance"

    val STRIPE_PLAN = "$BASE/stripe/plans"
    val STRIPE_CUSTOMER = "$BASE/stripe/customers"
    val STRIPE_SUB = "$BASE/stripe/subscriptions"
}

const val LAUNCH_SCHEDULE_URL = "https://api003.ftmailbox.com/index.php/jsapi/applaunchschedule"

const val FTC_OFFICIAL_URL = "http://www.ftchinese.com"
const val CN_FT = "https://cn.ft.com"

const val BASE_URL = FTC_OFFICIAL_URL

const val HOST_FTC = "www.ftchinese.com"
const val HOST_FT = "cn.ft.com"
const val HOST_FTA = "www.ftacademy.cn"

data class Flavor (
        var query: String,
        var baseUrl: String
) {
    val host: String
        get() = try {
            Uri.parse(baseUrl).host ?: ""
        } catch (e: Exception) {
            ""
        }
}

val defaultFlavor = Flavor(
        query = "an_ftc",
        baseUrl = FTC_OFFICIAL_URL
)

val flavors = mapOf(
        "play" to Flavor(
                query = "play_store",
                baseUrl = BASE_URL
        ),
        "xiaomi" to Flavor(
                query = "an_xiaomi",
                baseUrl = BASE_URL
        ),
        "huawei" to Flavor(
                query = "an_huawei",
                baseUrl = BASE_URL
        ),
        "baidu" to Flavor(
                query = "an_baidu",
                baseUrl = BASE_URL
        ),
        "sanliuling" to Flavor(
                query = "an_360shouji",
                baseUrl = BASE_URL
        ),
        "ftc" to defaultFlavor,
        "tencent" to Flavor(
                query = "an_tencent",
                baseUrl = BASE_URL
        ),
        "samsung" to Flavor(
                query = "an_samsung",
                baseUrl = BASE_URL
        ),
        "standard" to Flavor(
                query = "standard",
                baseUrl = BuildConfig.BASE_URL_STANDARD
        ),
        "premium" to Flavor(
                query = "premium",
                baseUrl = BuildConfig.BASE_URL_PREMIUM
        ),
        "b2b" to Flavor(
                query = "b2b",
                baseUrl = BuildConfig.BASE_URL_B2B
        )
)

val currentFlavor = flavors[BuildConfig.FLAVOR] ?: defaultFlavor
