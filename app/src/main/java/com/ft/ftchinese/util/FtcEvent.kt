package com.ft.ftchinese.util

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
}

object AdSlot {
    const val APP_OPEN = "launch screen"
}