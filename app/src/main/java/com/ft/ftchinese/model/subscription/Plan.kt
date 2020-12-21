package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.tracking.GAAction
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KTier
import kotlinx.parcelize.Parcelize
import org.threeten.bp.ZonedDateTime

/**
 * A plan for a product.
 */
@Parcelize
data class Plan(
        val id: String,
        val productId: String,
        val price: Double,
        @KTier
        val tier: Tier,
        @KCycle
        val cycle: Cycle,
        val description: String? = null,
        val currency: String = "cny", // Not from API
        val discount: Discount = Discount()
) : Parcelable {

    fun payableAmount(): Double {
        if (!discount.isValid()) {
            return price
        }

        return price - (discount.priceOff ?: 0.0)
    }

    fun getNamedKey(): String {
        return "${tier}_$cycle"
    }

    fun gaGAAction(): String {
        return when (tier) {
            Tier.STANDARD -> when (cycle) {
                Cycle.YEAR -> GAAction.BUY_STANDARD_YEAR
                Cycle.MONTH -> GAAction.BUY_STANDARD_MONTH
            }
            Tier.PREMIUM -> GAAction.BUY_PREMIUM
        }
    }
}

/**
 * Default pricing plans.
 */
val defaultPlans = listOf(
    Plan(
        id = "plan_ICMPPM0UXcpZ",
        productId = "prod_IxN4111S1TIP",
        price = 258.0,
        tier = Tier.STANDARD,
        cycle = Cycle.YEAR,
        description = "Standard Yearly Plan",
        discount = Discount(
            id = "dsc_UQKuPqxAZvmR",
            priceOff = 40.0,
            startUtc = ZonedDateTime.parse("2020-11-09T04:00:00Z"),
            endUtc = ZonedDateTime.parse("2020-11-11T16:00:00Z")
        )
    ),
    Plan(
        id = "plan_drbwQ2gTmtOK",
        productId = "prod_IxN4111S1TIP",
        price = 28.0,
        tier = Tier.STANDARD,
        cycle = Cycle.MONTH,
        description = "Standard Monthly Plan",
        discount = Discount()
    ),
    Plan(
        id = "plan_d6KVqcmEBqjv",
        productId = "prod_hNUYgnJR62Zt",
        price =  1998.0,
        tier = Tier.PREMIUM,
        cycle = Cycle.YEAR,
        description = "Premium Yearly Plan",
        discount = Discount(
            id = "dsc_cqgp9zBTwSnY",
            priceOff = 300.0,
            startUtc = ZonedDateTime.parse("2020-11-09T04:00:00Z"),
            endUtc = ZonedDateTime.parse("2020-11-11T16:00:00Z")
        )
    )
)

/**
 * PlanStore works as a in-memory cache of all plans.
 * This is kept for backward compatibility and many activities
 * use this to find out which plan a member is subscribed to.
 */
object PlanStore {
    // Will be updated once paywall data is fetched from server or cache.
    var plans = defaultPlans

    /**
     * Use to find out what plan an existing member is subscribed to,
     * or what plan an order is created for.
     */
    fun find(tier: Tier, cycle: Cycle): Plan? {
        return plans.find {
            it.tier == tier && it.cycle == cycle
        }
    }
}
