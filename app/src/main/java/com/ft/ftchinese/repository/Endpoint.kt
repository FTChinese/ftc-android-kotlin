package com.ft.ftchinese.repository

import android.net.Uri
import com.ft.ftchinese.BuildConfig

object NextApi {
    private val BASE = if (BuildConfig.DEBUG) {
        BuildConfig.API_READER_TEST
    } else {
        BuildConfig.API_READER_LIVE
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
        BuildConfig.API_CONTENT_LIVE
    } else {
        BuildConfig.API_CONTENT_LIVE
    }

    val STORY = "$BASE/stories"
    val INTERACTIVE = "$BASE/interactive/contents"
}

object SubscribeApi {
    private val BASE = if (BuildConfig.DEBUG) {
        BuildConfig.API_SUBS_TEST
    } else {
        BuildConfig.API_SUBS_LIVE
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

const val HOST_FTC = "www.ftchinese.com"
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

private const val OFFICIAL_URL = "http://$HOST_FTC"

val defaultFlavor = Flavor(
        query = "an_ftc",
        baseUrl = OFFICIAL_URL
)

val flavors = mapOf(
        "play" to Flavor(
                query = "play_store",
                baseUrl = OFFICIAL_URL
        ),
        "xiaomi" to Flavor(
                query = "an_xiaomi",
                baseUrl = OFFICIAL_URL
        ),
        "huawei" to Flavor(
                query = "an_huawei",
                baseUrl = OFFICIAL_URL
        ),
        "baidu" to Flavor(
                query = "an_baidu",
                baseUrl = OFFICIAL_URL
        ),
        "sanliuling" to Flavor(
                query = "an_360shouji",
                baseUrl = OFFICIAL_URL
        ),
        "ftc" to defaultFlavor,
        "tencent" to Flavor(
                query = "an_tencent",
                baseUrl = OFFICIAL_URL
        ),
        "samsung" to Flavor(
                query = "an_samsung",
                baseUrl = OFFICIAL_URL
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
