package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.order.StripeSubResponse

data class StripeSubscribedResult(
        val success: StripeSubResponse? = null,
        val isIdempotencyError: Boolean = false,
        val error: Int? = null,
        val exception: Exception? = null
)

