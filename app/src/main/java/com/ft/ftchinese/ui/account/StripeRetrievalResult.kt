package com.ft.ftchinese.ui.account

import com.ft.ftchinese.model.order.StripeSub

data class StripeRetrievalResult(
        val success: StripeSub? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
