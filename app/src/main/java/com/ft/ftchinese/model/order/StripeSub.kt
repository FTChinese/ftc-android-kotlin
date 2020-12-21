package com.ft.ftchinese.model.order

import com.ft.ftchinese.model.fetch.KDateTime
import com.ft.ftchinese.model.fetch.KStripeSubStatus
import org.threeten.bp.ZonedDateTime

/**
 * Handle initial payment outcome
 * 1. Payment success
 * 2. Payment failure
 * 3. Payment requires customer action.
 */
data class StripeSub(
        val cancelAtPeriodEnd: Boolean,
        @KDateTime
        val created: ZonedDateTime,
        @KDateTime
        val currentPeriodEnd: ZonedDateTime,
        @KDateTime
        val currentPeriodStart: ZonedDateTime,
        val latestInvoiceId: String,
        // lets us know whether we can provision the good or service associated with the subscription.
        @KStripeSubStatus
        val status: StripeSubStatus?
)
