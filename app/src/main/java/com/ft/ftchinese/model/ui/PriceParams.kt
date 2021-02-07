package com.ft.ftchinese.model.ui

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier

/**
 * Describes the params to build a human-readable price string
 */
data class PriceParams(
    val currency: String,
    val amount: Double,
    val cycle: Cycle,
    val tier: Tier,
)
