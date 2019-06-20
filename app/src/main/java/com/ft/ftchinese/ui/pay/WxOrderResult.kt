package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.WxPrepayOrder

data class WxOrderResult(
        val success: WxPrepayOrder? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
