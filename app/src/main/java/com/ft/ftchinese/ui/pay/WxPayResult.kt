package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.order.WxPaymentStatus

data class WxPayResult(
        val success: WxPaymentStatus? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
