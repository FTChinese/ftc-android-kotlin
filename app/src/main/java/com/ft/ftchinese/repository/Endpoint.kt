package com.ft.ftchinese.repository

object NextApi {
    private val BASE = Config.readerApiBase
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
    private val BASE = Config.contentApiBase

    val STORY = "$BASE/stories"
    val INTERACTIVE = "$BASE/interactive/contents"
}

object SubscribeApi {
    private val BASE = Config.subsApiProdBase

    val PAYWALL = "$BASE/paywall"

    val WX_LOGIN = "$BASE/wx/oauth/login"
    val WX_REFRESH = "$BASE/wx/oauth/refresh"

    val UPGRADE = "$BASE/upgrade/free"
    val UPGRADE_PREVIEW = "$BASE/upgrade/balance"

    val STRIPE_PLAN = "$BASE/stripe/plans"
    val STRIPE_CUSTOMER = "$BASE/stripe/customers"
    val STRIPE_SUB = "$BASE/stripe/subscriptions"

    private fun baseUrl(isTest: Boolean): String {
        return if (isTest) {
            Config.subsApiSandboxBase
        } else {
            Config.subsApiProdBase
        }
    }

    fun aliOrderUrl(isTest: Boolean): String {
        return "${baseUrl(isTest)}/alipay/app"
    }

    fun wxOrderUrl(isTest: Boolean): String {
        return "${baseUrl(isTest)}/wxpay/app"
    }

    fun verifyPaymentUrl(orderId: String, isTest: Boolean): String {
        return "${baseUrl(isTest)}/${orderId}/verify-payment"
    }
}

const val LAUNCH_SCHEDULE_URL = "https://api003.ftmailbox.com/index.php/jsapi/applaunchschedule"
