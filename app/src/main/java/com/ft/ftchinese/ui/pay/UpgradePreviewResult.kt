package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.order.UpgradePreview

data class UpgradePreviewResult (
        val success: UpgradePreview? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
