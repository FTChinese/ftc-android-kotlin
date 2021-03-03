package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.reader.Membership

data class VerificationResult(
    val order: Order,
    val payment: PaymentResult,
    val membership: Membership
)
