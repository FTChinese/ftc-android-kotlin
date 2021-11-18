package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.ftcsubs.Discount
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.stripesubs.StripePrice
import kotlinx.parcelize.Parcelize

@Parcelize
data class UnifiedPrice(
    val id: String,
    override val tier: Tier,
    override val cycle: Cycle,
    val active: Boolean = true,
    val currency: String,
    val isIntroductory: Boolean,
    val liveMode: Boolean,
    val nickname: String,
    val offerDesc: String?,
    val productId: String,
    val periodDays: Int,
    val source: PriceSource,
    val unitAmount: Double,
    val ftcDiscountId: String?,
    val stripePriceId: String,
) : Edition(
    tier = tier,
    cycle = cycle,
), Parcelable {
    companion object {
        @JvmStatic
        fun fromStripe(p: StripePrice): UnifiedPrice {
            return UnifiedPrice(
                id = p.id,
                tier = p.metadata.tier,
                cycle = p.cycle,
                active = p.active,
                currency = p.currency,
                isIntroductory = p.isIntroductory,
                liveMode = p.liveMode,
                nickname = p.nickname,
                offerDesc = null,
                productId = p.product,
                periodDays = p.metadata.periodDays,
                source = PriceSource.Stripe,
                unitAmount = p.moneyAmount,
                ftcDiscountId = null,
                stripePriceId = p.id,
            )
        }

        @JvmStatic
        fun fromFtc(p: Price, discount: Discount?): UnifiedPrice {
            return UnifiedPrice(
                id = p.id,
                tier = p.tier,
                cycle = p.cycle,
                active = p.active,
                currency = p.currency,
                isIntroductory = discount?.isIntroductory() ?: false,
                liveMode = p.liveMode,
                nickname = p.nickname ?: "",
                offerDesc = discount?.description,
                productId = p.productId,
                periodDays = 0,
                source = PriceSource.Ftc,
                unitAmount = discount?.let {
                    p.unitAmount - (it.priceOff ?: 0.0)
                } ?: p.unitAmount,
                ftcDiscountId = discount?.id,
                stripePriceId = p.stripePriceId,
            )
        }
    }
}
