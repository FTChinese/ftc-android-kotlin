package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.fetch.KDateTime
import org.threeten.bp.ZonedDateTime

data class PaymentIntent(
    val id: String,
    val amount: Double,
//    @KDateTime
//    val canceledAtUtc: ZonedDateTime? = null,
    val cancellationReason: String,
    val clientSecret: String?,
//    @KDateTime
//    val createdUtc: ZonedDateTime? = null,
    val currency: String,
    val customerId: String,
    val invoiceId: String,
    val liveMode: Boolean,
    val paymentMethodId: String,
    val status: String,
) {
    val requiresAction: Boolean
        get() = status == "requires_action"
}
