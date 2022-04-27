package com.ft.ftchinese.model.iapsubs

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import kotlinx.serialization.Serializable

@Serializable
data class IapSubs(
    val originalTransactionId: String,
    val purchaseDateUtc: String?,
    val expiresDateUtc: String?,
    val tier: Tier,
    val cycle: Cycle,
    val autoRenewal: Boolean,
    val createdUtc: String?,
    val updatedUtc: String?,
    val ftcUserId: String?,
    val inUse: Boolean = false
)
