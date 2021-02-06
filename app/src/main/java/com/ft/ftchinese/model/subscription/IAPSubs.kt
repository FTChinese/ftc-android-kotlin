package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KTier

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
