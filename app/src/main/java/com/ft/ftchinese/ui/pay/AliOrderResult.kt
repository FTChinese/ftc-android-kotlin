package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.AliOrder

data class AliOrderResult(
        val success: AliOrder? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
