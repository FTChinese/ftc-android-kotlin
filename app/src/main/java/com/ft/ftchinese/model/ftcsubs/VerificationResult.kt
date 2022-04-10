package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.reader.Membership
import kotlinx.serialization.Serializable

@Serializable
data class VerificationResult(
    val order: Order,
    val payment: PaymentResult,
    val membership: Membership
)
