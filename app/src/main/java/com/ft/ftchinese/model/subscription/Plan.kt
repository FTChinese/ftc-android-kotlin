package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.tracking.GAAction
import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KTier
import kotlinx.android.parcel.Parcelize
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
        val description: String,
        val currency: String = "cny", // Not from API
        val discount: Discount
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

    fun paymentIntent(kind: OrderUsage?): PaymentIntent {
        return PaymentIntent(
                amount = payableAmount(),
                currency = currency,
                subscriptionKind = kind,
                plan = this
        )
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
            id = "dsc_PqU34aIHEErX",
            priceOff = 130.0,
            percent = null,
            startUtc = ZonedDateTime.parse("2020-08-18T16:00:00Z"),
            endUtc = ZonedDateTime.parse("2020-09-02T16:00:00Z"),
            description = null
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
        id = "plan_5iIonqaehig4",
        productId = "prod_dcHBCHaBTn3w",
        price =  1998.0,
        tier = Tier.PREMIUM,
        cycle = Cycle.YEAR,
        description = "Premium Yearly Plan",
        discount = Discount(
            id = "dsc_hwvNK0Cfyiny",
            priceOff = 1000.0,
            percent = null,
            startUtc = ZonedDateTime.parse("2020-08-18T16:00:00Z"),
            endUtc = ZonedDateTime.parse("2020-09-02T16:00:00Z"),
            description =  null
        )
    )
)

object PlanStore {
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
