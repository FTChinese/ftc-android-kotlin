package com.ft.ftchinese.model.stripesubs

data class PriceRecurring(
    val interval: String, // week, month, year
    val intervalCount: Int,
    val usageType: String, // licensed, metered
)
