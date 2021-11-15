package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KTier

data class SubParams(
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle,
    val priceId: String,
    val customer: String, // Deprecated
    val coupon: String? = null,
    val defaultPaymentMethod: String?,
    val idempotency: String? = null
)
