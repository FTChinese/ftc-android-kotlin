package com.ft.ftchinese.model

data class Product (
        val heading: String,
        val benefits: Array<String>,
        val smallPrint: String? = null,
        val tier: Tier,
        val currency: String,
        val plans: Array<Plan>
)
