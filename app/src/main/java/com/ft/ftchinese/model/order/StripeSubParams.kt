package com.ft.ftchinese.model.order

import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KTier

data class StripeSubParams(
        @KTier
        val tier: Tier,
        @KCycle
        val cycle: Cycle,
        val customer: String,
        val coupon: String? = null,
        val defaultPaymentMethod: String?,
        val idempotency: String
)
