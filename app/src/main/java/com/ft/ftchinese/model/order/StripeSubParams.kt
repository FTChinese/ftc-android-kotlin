package com.ft.ftchinese.model.order

import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KTier

data class StripeSubParams(
        @KTier
        val tier: Tier,
        @KCycle
        val cycle: Cycle,
        val customer: String, // Deprecated
        val coupon: String? = null,
        val defaultPaymentMethod: String?,
        val idempotency: String? = null
)
