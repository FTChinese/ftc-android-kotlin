package com.ft.ftchinese.model.order

import com.beust.klaxon.Json

data class StripeSub(
        val id: String,
        val created: Long,
        @Json(name = "current_period_start")
        val star: Long,
        @Json(name = "current_period_end")
        val end: Long,
        val customer: String,
        @Json(name = "days_until_due")
        val daysUntilDue: Int,
        val latestInvoice: StripeInvoice
)
