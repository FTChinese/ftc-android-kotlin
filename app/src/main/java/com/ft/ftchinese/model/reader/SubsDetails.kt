package com.ft.ftchinese.model.reader

import com.ft.ftchinese.model.enums.Cycle

enum class SubsTier {
    Free,
    Standard,
    Premium,
    Vip,
}

data class ExpirationMoment(
    val unlimited: Boolean,
    val autoRenew: Boolean,
    val interval: Cycle?,
    val year: Int,
    val month: Int,
    val date: Int,
)

data class SubsDetails(
    val tier: SubsTier,
    val expiration: ExpirationMoment
)
