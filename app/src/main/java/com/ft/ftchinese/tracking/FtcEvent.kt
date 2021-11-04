package com.ft.ftchinese.tracking

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.price.Edition

/**
 * Custom parameter name used for Firebase
 */
object FtcEvent {
    // when a user clicks an ad.
    const val AD_CLICK = "ftc_ad_click"
    // when at least one ad is on screen.
    const val AD_EXPOSURE = "ftc_ad_exposure"
    // when a user sees an ad impression.
    const val AD_IMPRESSION = "ftc_ad_impression"
    // when an ad is showed to user without being manually skipped.
    const val AD_VIEWED = "ftc_ad_viewed"
    // When a user skips an ad.
    const val AD_SKIP = "ftc_ad_skip"
    const val PAYWALL_FROM = "pay_wall_source"
}

object AdSlot {
    const val APP_OPEN = "launch screen"
}

object GACategory {
    const val APP_LAUNCH = "Android App Launch"
    const val SUBSCRIPTION = "Android Privileges"
    const val LAUNCH_AD = "Android Launch Ad"
}

object GAAction {
    const val SUCCESS = "Success"
    const val DISPLAY = "Display"
    const val TAP = "Tap"
    const val BUY_STANDARD_YEAR = "Buy: ftc_standard"
    const val BUY_STANDARD_SUCCESS = "Buy success: ftc_standard"
    const val BUY_STANDARD_FAIL = "Buy fail: ftc_standard"
    const val BUY_STANDARD_MONTH = "Buy: ftc_standard_month"
    const val BUY_PREMIUM = "Buy: ftc_premium"
    const val BUY_PREMIUM_SUCCESS = "Buy success: ftc_premium"
    const val BUY_PREMIUM_FAIL = "Buy fail: ftc_premium"
    const val LAUNCH_AD_SENT = "Request"
    const val LAUNCH_AD_SUCCESS = "Sent"
    const val LAUNCH_AD_FAIL = "Fail"
    const val LAUNCH_AD_CLICK = "Click"

    fun get(e: Edition): String {
        return when (e.tier) {
            Tier.STANDARD -> when (e.cycle) {
                Cycle.YEAR -> BUY_STANDARD_YEAR
                Cycle.MONTH -> BUY_STANDARD_MONTH
            }
            Tier.PREMIUM -> BUY_PREMIUM
        }
    }
}
