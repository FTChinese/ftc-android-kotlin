package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KTier
import kotlinx.parcelize.Parcelize

@Parcelize
data class StripePrice(
    val id: String,
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle,
    val active: Boolean = true,
    val currency: String,
    val liveMode: Boolean,
    val nickname: String? = null,
    val productId: String,
    val unitAmount: Int,
    val amount: Int, // Deprecated
    val interval: String // Deprecated
) : Parcelable {

    fun humanAmount(): Double {
        return (amount / 100).toDouble()
    }
}

object StripePriceStore {
    var prices = listOf<StripePrice>()

    fun find(tier: Tier, cycle: Cycle): StripePrice? {
        return prices.find {
            it.tier == tier && it.cycle == cycle
        }
    }
}

