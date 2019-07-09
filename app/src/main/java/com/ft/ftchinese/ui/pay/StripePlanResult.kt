package com.ft.ftchinese.ui.pay

import com.ft.ftchinese.model.order.StripePlan

data class StripePlanResult(
        val success: StripePlan? = null,
        val error: Int? = null,
        val exception: Exception? = null
)
