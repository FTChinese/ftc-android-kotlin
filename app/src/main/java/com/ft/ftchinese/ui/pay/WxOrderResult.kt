package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.order.WxOrder

data class WxOrderResult(
        val success: WxOrder? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
