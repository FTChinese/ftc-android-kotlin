package com.ft.ftchinese.model.order

data class StripePlan(
        val active: Boolean = true,
        val amount: Int,
        val currency: String,
        val interval: String
)
