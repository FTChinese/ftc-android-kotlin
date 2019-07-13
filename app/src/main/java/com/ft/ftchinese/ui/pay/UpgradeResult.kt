package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.order.UpgradePreview

data class UpgradeResult(
        val success: Boolean = false,
        val preview: UpgradePreview? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
