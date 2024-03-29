package com.ft.ftchinese.model.stripesubs

import kotlinx.serialization.Serializable

@Serializable
data class PaymentIntent(
    val id: String,
    val amount: Double,
    val cancellationReason: String,
    val clientSecret: String?,
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
