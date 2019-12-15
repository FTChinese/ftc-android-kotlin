package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.model.order.OrderUsage
import com.ft.ftchinese.util.KOrderUsage
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PaymentIntent(
        val amount: Double,
        val currency: String,
        val cycleCount: Int = 1,
        val extraDays: Int = 1,
        @KOrderUsage
        val subscriptionKind: OrderUsage? = null,
        val wallet: Wallet = Wallet(),
        val plan: Plan
) : Parcelable {
    fun currencySymbol(): String {
        return plan.currencySymbol()
    }

    fun isPayRequired(): Boolean {
        return amount != 0.0
    }
}
