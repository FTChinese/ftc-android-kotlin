package com.ft.ftchinese.model.order

import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KDateTime
import com.ft.ftchinese.model.fetch.KStripeSubStatus
import com.ft.ftchinese.model.fetch.KTier
import org.threeten.bp.ZonedDateTime

data class StripePaymentResult(
    val requiresAction: Boolean,
    val paymentIntentClientSecret: String? = null
)

data class StripeSubs(
    val id: String,
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle,
    @KDateTime
    val cancelAtUtc: ZonedDateTime? = null,
    val cancelAtPeriodEnd: Boolean,
    @KDateTime
    val canceledUtc: ZonedDateTime? = null,
    @KDateTime
    val currentPeriodEnd: ZonedDateTime,
    @KDateTime
    val currentPeriodStart: ZonedDateTime,
    val customerId: String,
    val defaultPaymentMethod: String? = null,
    val subsItemId: String,
    val priceId: String,
    val latestInvoiceId: String,
    val liveMode: Boolean,
    @KDateTime
    val startDateUtc: ZonedDateTime? = null,
    @KDateTime
    val endedUtc: ZonedDateTime? = null,
    @KDateTime
    val createdUtc: ZonedDateTime? = null,
    @KDateTime
    val updatedUtc: ZonedDateTime? = null,
    @KStripeSubStatus
    val status: StripeSubStatus? = null,
    val ftcUserId: String? = null
)

data class StripeSubResult(
    val payment:  StripePaymentResult,
    val subs: StripeSubs,
    val membership: Membership
)
