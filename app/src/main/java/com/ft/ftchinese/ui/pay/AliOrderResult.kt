package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.AlipayOrder

data class AliOrderResult(
        val success: AlipayOrder? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
