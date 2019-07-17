package com.ft.ftchinese.model.order

import com.ft.ftchinese.util.KDateTime
import com.ft.ftchinese.util.KStripeSubStatus
import org.threeten.bp.ZonedDateTime


data class StripeSub(
        val cancelAtPeriodEnd: Boolean,
        @KDateTime
        val created: ZonedDateTime,
        @KDateTime
        val currentPeriodEnd: ZonedDateTime,
        @KDateTime
        val currentPeriodStart: ZonedDateTime,
        @KDateTime
        val endedAt: ZonedDateTime? = null,
        val latestInvoice: StripeInvoice,
        @KStripeSubStatus
        val status: StripeSubStatus?
)
