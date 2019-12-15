package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.subscription.PaymentIntent

data class UpgradePreviewResult (
        val success: PaymentIntent? = null,
        val errorId: Int? = null,
        val exception: Exception? = null
)
