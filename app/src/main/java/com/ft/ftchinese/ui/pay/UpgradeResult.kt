package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.subscription.PaymentIntent

data class UpgradeResult(
        val success: Boolean = false,
        val preview: PaymentIntent? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
