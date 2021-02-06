package com.ft.ftchinese.model.ui

import com.ft.ftchinese.model.enums.Cycle

/**
 * Describes the params to build a human-readable price string
 */
data class Price(
    val currency: String,
    val amount: Double,
    val cycle: Cycle,
)
