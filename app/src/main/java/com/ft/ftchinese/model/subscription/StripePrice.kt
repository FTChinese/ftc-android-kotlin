package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KTier
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
) : Parcelable {

    val edition: Edition
        get() = Edition(tier, cycle)

    fun humanAmount(): Double {
        return (unitAmount / 100).toDouble()
    }
}

// In-memory cache of stripe prices. The data is also persisted to cache file.
// See FileCache.kt for cached file name
object StripePriceStore {
    var prices = listOf<StripePrice>()

    fun findById(id: String): StripePrice? {
        return prices.find { it.id == id }
    }

    fun find(tier: Tier, cycle: Cycle): StripePrice? {
        return prices.find {
            it.tier == tier && it.cycle == cycle
        }
    }
}

