package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KTier

data class PriceMetadata(
    @KTier
    val tier: Tier,
    val periodDays: Int,
    val introductory: Boolean,
)
