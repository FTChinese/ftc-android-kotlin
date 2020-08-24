package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.util.KTier

data class Product(
    val id: String,
    @KTier
    val tier: Tier,
    val heading: String,
    val description: String?,
    val smallPrint: String?,
    val plans: List<Plan>
)
