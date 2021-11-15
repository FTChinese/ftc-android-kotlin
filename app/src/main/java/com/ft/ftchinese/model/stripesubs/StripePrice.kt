package com.ft.ftchinese.model.stripesubs

data class StripePrice(
    val active: Boolean,
    val created: Int,
    val currency: String,
    val id: String,
    val liveMode: Boolean,
    val metadata: PriceMetadata,
    val nickname: String,
    val product: String,
    val recurring: PriceRecurring,
    val type: String, // one_time, recurring.
    val unitAmount: Int,
) {
    val isIntroductory: Boolean
        get() = type == "one_time" && metadata.introductory
}
