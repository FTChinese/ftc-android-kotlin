package com.ft.ftchinese.model.ftcsubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.fetch.*
import kotlinx.parcelize.Parcelize
import org.threeten.bp.ZonedDateTime

/**
 * Price contains data of a product's price.
 * It unifies both ftc and Stripe product.
 */
@Parcelize
data class Price(
    val id: String,
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle?,
    val active: Boolean = true,
    val currency: String = "cny",
    @KPriceKind
    val kind: PriceKind? = null,
    val liveMode: Boolean,
    val nickname: String? = null,
    val periodCount: YearMonthDay = YearMonthDay(),
    val productId: String,
    val stripePriceId: String,
    val title: String? = null,
    val unitAmount: Double,
    @KDateTime
    val startUtc: ZonedDateTime? = null,
    @KDateTime
    val endUtc: ZonedDateTime? = null,
    val offers: List<Discount> = listOf(), // Does not exist when used as introductory price.
) : Parcelable {

    fun toJsonString(): String {
        return json.toJsonString(this)
    }

    fun isAnnual(): Boolean {
        return periodCount.years > 0
    }

    fun isMonthly(): Boolean {
        return periodCount.months > 0
    }

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
        val ymd = normalizeYMD()
        val c = ymd.toCycle()
        val days = if (ymd.isZero()) {
            1
        } else {
            ymd.totalDays()
        }

        val avg = unitAmount.div(days)

        return when (c) {
            Cycle.YEAR -> DailyPrice.ofYear(avg)
            Cycle.MONTH -> DailyPrice.ofMonth(avg)
        }
    }

    /**
     * Find out the most applicable discount a membership
     * could enjoy among this price's offers.
     */
    fun applicableOffer(filters: List<OfferKind>): Discount? {
        if (offers.isEmpty()) {
            return null
        }

        // Filter the offers that are in the filters
        // list. If there are multiple ones, use
        // the one with the biggest price off.
        // Ignore introductory discount which is no longer used.
        val filtered = offers.filter {
                it.kind != OfferKind.Introductory && it.isValid() && filters.contains(it.kind)
            }
            .sortedByDescending {
                it.priceOff
            }

        if (filtered.isNullOrEmpty()) {
            return null
        }

        return filtered[0]
    }

}
