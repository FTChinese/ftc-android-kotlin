package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KTier

data class IAPSubs(
    val originalTransactionId: String,
    val purchaseDateUtc: String?,
    val expiresDateUtc: String?,
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle,
    val autoRenewal: Boolean,
    val createdUtc: String?,
    val updatedUtc: String?,
    val ftcUserId: String?,
    val inUse: Boolean = false
)
