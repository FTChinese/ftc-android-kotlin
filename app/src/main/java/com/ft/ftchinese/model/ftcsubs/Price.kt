package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.threeten.bp.ZonedDateTime

/**
 * Price contains data of a product's price.
 * It unifies both ftc and Stripe product.
 */
@Serializable
data class Price(
    val id: String,
    val tier: Tier,
    val cycle: Cycle?,
    val active: Boolean = true,
    val currency: String = "cny",
    val kind: PriceKind? = null,
    val liveMode: Boolean,
    val nickname: String? = null,
    val periodCount: YearMonthDay = YearMonthDay(),
    val productId: String,
    val stripePriceId: String,
    val title: String? = null,
    val unitAmount: Double,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endUtc: ZonedDateTime? = null,
    val offers: List<Discount> = listOf(), // Does not exist when used as introductory price.
) {

    fun toJsonString(): String {
        return Json.encodeToString(this)
    }

    val isIntro: Boolean
        get() = kind == PriceKind.OneTime

    val edition: Edition
        get() = Edition(
            tier = tier,
            cycle = cycle ?: periodCount.toCycle()
        )

    fun normalizeYMD(): YearMonthDay {
        if (!periodCount.isZero()) {
            return periodCount
        }

        return cycle?.let {
            YearMonthDay.of(it)
        }
            ?: YearMonthDay.zero()
    }

    fun isValid(): Boolean {
        if (startUtc == null || endUtc == null) {
            return true
        }

        val now = ZonedDateTime.now()

        if (now.isBefore(startUtc) || now.isAfter(endUtc)) {
            return false
        }

        return true
    }

    fun dailyPrice(): DailyPrice {
        val days = if (periodCount.isZero()) {
            1
        } else {
            periodCount.totalDays()
        }

        val avg = unitAmount.div(days)

        return when (periodCount.toCycle()) {
            Cycle.YEAR -> DailyPrice.ofYear(avg)
            Cycle.MONTH -> DailyPrice.ofMonth(avg)
        }
    }

    /**
     * Find out the most applicable discount a membership
     * could enjoy among this price's offers.
     */
    fun filterOffer(by: List<OfferKind>): Discount? {
        if (offers.isEmpty()) {
            return null
        }

        // Filter the offers that are in the filters
        // list. If there are multiple ones, use
        // the one with the biggest price off.
        // Ignore introductory discount which is no longer used.
        val filtered = offers.filter {
                it.kind != OfferKind.Introductory && it.isValid() && by.contains(it.kind)
            }
            .sortedByDescending {
                it.priceOff
            }

        if (filtered.isEmpty()) {
            return null
        }

        return filtered[0]
    }

}
