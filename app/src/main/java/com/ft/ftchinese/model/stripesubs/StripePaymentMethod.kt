package com.ft.ftchinese.model.stripesubs

data class StripePaymentMethod(
    val id: String,
    val customerId: String,
    val card: StripePaymentCard,
)

data class StripePaymentCard(
    val brand: String,
    val country: String,
    val expMonth: Int,
    val expYear: Int,
    val last4: String,
)
