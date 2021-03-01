package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KTier
import com.ft.ftchinese.model.price.Price

/**
 * Defines the data to present product on paywall.
 * By default the data is hard-coded in string resources.
 */
data class Product(
    val id: String,
    @KTier
    val tier: Tier,
    val heading: String,
    val description: String?,
    val smallPrint: String?,
    val prices: List<Price>,
)
