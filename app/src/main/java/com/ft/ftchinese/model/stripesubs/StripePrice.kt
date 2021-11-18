package com.ft.ftchinese.model.stripesubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Cycle
import kotlinx.parcelize.Parcelize

@Parcelize
data class StripePrice(
    val id: String,
    val active: Boolean,
    val created: Int,
    val currency: String,
    val liveMode: Boolean,
    val metadata: PriceMetadata,
    val nickname: String,
    val product: String,
    val recurring: PriceRecurring,
    val type: String, // one_time, recurring.
    val unitAmount: Int,
) : Parcelable {
    val isIntroductory: Boolean
        get() = type == "one_time" && metadata.introductory

    val moneyAmount: Double
        get() = unitAmount
            .toBigDecimal()
            .divide(
                100.toBigDecimal()
            )
            .toDouble()

    val cycle: Cycle
        get() = when {
            recurring.interval != null -> recurring.interval
            else -> when (metadata.periodDays) {
                in 30..31 -> Cycle.MONTH
                in 365..366 -> Cycle.YEAR
                else -> Cycle.YEAR
            }
        }
}
