package com.ft.ftchinese.model.stripesubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import kotlinx.serialization.Serializable
import kotlinx.parcelize.Parcelize

/** The price and plan that will apply at the next Stripe renewal. */
@Serializable
@Parcelize
data class StripeRenewal(
    val effectiveAt: String? = null,
    val priceId: String = "",
    val tier: Tier? = null,
    val cycle: Cycle? = null,
    val currency: String = "",
    val originalAmountMinor: Int = 0,
    val amountMinor: Int = 0,
    val discountPercent: Double? = null,
    val discountAmountMinor: Int = 0,
) : Parcelable
