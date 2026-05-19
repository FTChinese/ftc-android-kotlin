package com.ft.ftchinese.ui.subs.catalog

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.StripeSubStatus
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePendingChange
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogOption
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogPlan
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogProduct
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCheckout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime

class CatalogCheckoutBuilderTest {

    @Test
    fun stripeDowngradeDoesNotCarryCouponOrPaymentMethod() {
        val item = buildCatalogStripeCartItem(
            membership = Membership(
                tier = Tier.PREMIUM,
                cycle = Cycle.YEAR,
                expireDate = LocalDate.now().plusMonths(6),
                payMethod = PayMethod.STRIPE,
                stripeSubsId = "sub_123",
                autoRenew = true,
                status = StripeSubStatus.Active,
            ),
            product = SubscriptionCatalogProduct(
                id = "standard",
                tier = Tier.STANDARD,
                name = "标准会员",
            ),
            plan = SubscriptionCatalogPlan(
                id = "standard-year",
                cycle = "year",
                title = "年度订阅",
            ),
            option = SubscriptionCatalogOption(
                id = "standard-year-stripe",
                kind = "stripe",
                displayPrice = "TWD1117.50 / 年",
                originalPrice = "TWD1490.00 / 年",
                checkout = SubscriptionCheckout(
                    stripePriceId = "price_standard_twd",
                    stripeCurrency = "twd",
                    stripeUnitAmount = 149000,
                    stripePayableAmount = 111750,
                    stripeCouponAmountOff = 37250,
                    stripeCouponId = "coupon_standard_twd",
                )
            )
        )

        requireNotNull(item)
        assertEquals(IntentKind.Downgrade, item.intent.kind)
        assertNull(item.coupon)
        assertFalse(item.requiresPaymentMethod)

        val params = item.subsParams(payMethod = null)
        assertEquals("price_standard_twd", params.priceId)
        assertEquals("twd", params.currency)
        assertNull(params.coupon)
        assertNull(params.defaultPaymentMethod)
    }

    @Test
    fun cancelScheduledDowngradeDoesNotCarryCouponOrPaymentMethod() {
        val item = buildCatalogStripeCartItem(
            membership = Membership(
                tier = Tier.PREMIUM,
                cycle = Cycle.YEAR,
                expireDate = LocalDate.now().plusMonths(6),
                payMethod = PayMethod.STRIPE,
                stripeSubsId = "sub_123",
                autoRenew = true,
                status = StripeSubStatus.Active,
                pendingStripeChange = StripePendingChange(
                    kind = "downgrade",
                    scheduleId = "sched_123",
                    targetTier = Tier.STANDARD,
                    targetCycle = Cycle.YEAR,
                    targetPriceId = "price_standard_twd",
                    effectiveAt = ZonedDateTime.now().plusMonths(6),
                )
            ),
            product = SubscriptionCatalogProduct(
                id = "premium",
                tier = Tier.PREMIUM,
                name = "高端会员",
            ),
            plan = SubscriptionCatalogPlan(
                id = "premium-year",
                cycle = "year",
                title = "年度订阅",
            ),
            option = SubscriptionCatalogOption(
                id = "premium-year-stripe",
                kind = "stripe",
                displayPrice = "TWD6375.00 / 年",
                originalPrice = "TWD8500.00 / 年",
                checkout = SubscriptionCheckout(
                    stripePriceId = "price_premium_twd",
                    stripeCurrency = "twd",
                    stripeUnitAmount = 850000,
                    stripePayableAmount = 637500,
                    stripeCouponAmountOff = 212500,
                    stripeCouponId = "coupon_premium_twd",
                )
            )
        )

        requireNotNull(item)
        assertEquals(IntentKind.CancelScheduledChange, item.intent.kind)
        assertNull(item.coupon)
        assertFalse(item.requiresPaymentMethod)

        val params = item.subsParams(payMethod = null)
        assertEquals("price_premium_twd", params.priceId)
        assertEquals("twd", params.currency)
        assertNull(params.coupon)
        assertNull(params.defaultPaymentMethod)
    }

    @Test
    fun stripeCurrencyFallbackRecognizesNewTaiwanDollarDisplay() {
        val item = buildCatalogStripeCartItem(
            membership = Membership(),
            product = SubscriptionCatalogProduct(
                id = "standard",
                tier = Tier.STANDARD,
                name = "标准会员",
            ),
            plan = SubscriptionCatalogPlan(
                id = "standard-year",
                cycle = "year",
                title = "年度订阅",
            ),
            option = SubscriptionCatalogOption(
                id = "standard-year-stripe",
                kind = "stripe",
                displayPrice = "NT$1117.50 / 年",
                originalPrice = "NT$1490.00 / 年",
                checkout = SubscriptionCheckout(
                    stripePriceId = "price_standard_twd",
                    stripeCouponId = "coupon_standard_twd",
                )
            )
        )

        requireNotNull(item)
        assertEquals("twd", item.recurring.currency)
        assertNotNull(item.coupon)
        assertEquals("twd", item.coupon?.currency)
        assertEquals(149000, item.recurring.unitAmount)
        assertEquals(37250, item.coupon?.amountOff)
    }
}
