package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.model.fetch.KOrderUsage
import kotlinx.parcelize.Parcelize

@Parcelize
data class PaymentIntent(
    val amount: Double,
    val currency: String,
    val cycleCount: Int = 1,
    val extraDays: Int = 1,
    @KOrderUsage
    val kind: OrderKind? = null,
    val wallet: Wallet = Wallet(),
    val plan: Plan
) : Parcelable {

    fun isPayRequired(): Boolean {
        return amount != 0.0
    }
}
