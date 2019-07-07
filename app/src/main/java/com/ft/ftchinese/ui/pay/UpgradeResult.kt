package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.order.PlanPayable

data class UpgradeResult(
        val success: Boolean = false,
        val plan: PlanPayable? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
