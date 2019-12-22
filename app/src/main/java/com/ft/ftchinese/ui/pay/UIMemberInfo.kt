package com.ft.ftchinese.ui.pay

data class UIMemberInfo(
        val tier: String,
        val expireDate: String,
        val autoRenewal: Boolean,
        val stripeStatus: String?, // Show stripe status or hide it if null
        val stripeInactive: Boolean, // Show stripe status warning
        val remains: String? // Show remaining days that will expire or expired.
)
