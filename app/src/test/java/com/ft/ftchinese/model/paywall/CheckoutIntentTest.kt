package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.StripeSubStatus
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePendingChange
import com.ft.ftchinese.model.stripesubs.StripePrice
import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime

class CheckoutIntentTest {

    @Test
    fun stripeMemberCannotRepeatSameTierWithFtcPay() {
        val intent = CheckoutIntent.ofFtc(
            source = stripeStandard,
            target = price(Tier.STANDARD)
        )

        assertEquals(IntentKind.Forbidden, intent.kind)
    }

    @Test
    fun stripeMemberCannotUpgradeWithFtcPay() {
        val intent = CheckoutIntent.ofFtc(
            source = stripeStandard,
            target = price(Tier.PREMIUM)
        )

        assertEquals(IntentKind.Forbidden, intent.kind)
    }

    @Test
    fun appleMemberCannotPurchaseFromAndroidFtcPay() {
        val intent = CheckoutIntent.ofFtc(
            source = Membership(
                tier = Tier.STANDARD,
                cycle = Cycle.YEAR,
                expireDate = LocalDate.now().plusMonths(3),
                payMethod = PayMethod.APPLE,
                appleSubsId = "apple_123",
                autoRenew = true,
            ),
            target = price(Tier.PREMIUM)
        )

        assertEquals(IntentKind.Forbidden, intent.kind)
    }

    @Test
    fun pendingStripeDowngradeCanBeCancelledByChoosingCurrentTier() {
        val intent = CheckoutIntent.ofStripe(
            source = stripePremiumWithPendingDowngrade,
            target = stripePrice(Tier.PREMIUM),
        )

        assertEquals(IntentKind.CancelScheduledChange, intent.kind)
    }

    @Test
    fun pendingStripeDowngradeCannotBeScheduledAgain() {
        val intent = CheckoutIntent.ofStripe(
            source = stripePremiumWithPendingDowngrade,
            target = stripePrice(Tier.STANDARD),
        )

        assertEquals(IntentKind.Forbidden, intent.kind)
    }

    @Test
    fun cancelledStripePremiumCannotScheduleFutureDowngrade() {
        val intent = CheckoutIntent.ofStripe(
            source = stripePremiumCancelled,
            target = stripePrice(Tier.STANDARD),
        )

        assertEquals(IntentKind.Forbidden, intent.kind)
    }

    private companion object {
        val stripeStandard = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(3),
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_123",
            autoRenew = true,
            status = StripeSubStatus.Active,
        )

        val stripePremiumWithPendingDowngrade = Membership(
            tier = Tier.PREMIUM,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(3),
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_123",
            autoRenew = true,
            status = StripeSubStatus.Active,
            pendingStripeChange = StripePendingChange(
                kind = "downgrade",
                scheduleId = "sched_123",
                targetTier = Tier.STANDARD,
                targetCycle = Cycle.YEAR,
                targetPriceId = "price_standard",
                effectiveAt = ZonedDateTime.now().plusMonths(3),
            )
        )

        val stripePremiumCancelled = Membership(
            tier = Tier.PREMIUM,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(3),
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_123",
            autoRenew = false,
            status = StripeSubStatus.Canceled,
        )

        fun price(tier: Tier): Price {
            return Price(
                id = "${tier.symbol}_year_cny",
                tier = tier,
                cycle = Cycle.YEAR,
                liveMode = true,
                periodCount = YearMonthDay.of(Cycle.YEAR),
                productId = tier.symbol,
                stripePriceId = "",
                title = tier.symbol,
                unitAmount = if (tier == Tier.PREMIUM) 1698.0 else 268.0,
            )
        }

        fun stripePrice(tier: Tier): StripePrice {
            return StripePrice(
                id = "price_${tier.symbol}",
                active = true,
                currency = "twd",
                liveMode = true,
                nickname = tier.symbol,
                periodCount = YearMonthDay.of(Cycle.YEAR),
                productId = tier.symbol,
                tier = tier,
                unitAmount = if (tier == Tier.PREMIUM) 637500 else 111750,
            )
        }
    }
}
