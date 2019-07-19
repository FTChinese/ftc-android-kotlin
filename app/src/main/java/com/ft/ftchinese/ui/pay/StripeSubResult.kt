package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.order.StripeSub

data class StripeSubResult(
        val success: StripeSub? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
