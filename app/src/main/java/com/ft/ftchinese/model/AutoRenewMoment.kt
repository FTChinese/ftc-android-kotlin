package com.ft.ftchinese.model

import com.ft.ftchinese.model.enums.Cycle

data class AutoRenewMoment(
    val cycle: Cycle,
    val month: Int?,
    val date: Int,
)
