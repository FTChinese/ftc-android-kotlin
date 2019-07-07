package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.order.Order

data class OrderResult(
        val success: Order? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
