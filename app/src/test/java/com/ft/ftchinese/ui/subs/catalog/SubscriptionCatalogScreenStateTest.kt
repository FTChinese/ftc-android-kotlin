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
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogSummary
import com.ft.ftchinese.ui.subs.stripeAutoRenewUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime

class SubscriptionCatalogScreenStateTest {

    private val standardProduct = SubscriptionCatalogProduct(
        id = "standard",
        tier = Tier.STANDARD,
        name = "标准会员",
        plans = listOf(standardYear)
    )

    private val premiumProduct = SubscriptionCatalogProduct(
        id = "premium",
        tier = Tier.PREMIUM,
        name = "高端会员",
        plans = listOf(premiumYear)
    )

    @Test
    fun stripeCurrentTierCannotBePurchasedAgain() {
        val membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_123",
            autoRenew = true,
            status = StripeSubStatus.Active,
        )

        val state = planCheckoutState(standardProduct, standardYear, membership, "zh-TW")
        val choices = paymentChoicesForPlan(standardProduct, standardYear, membership, "zh-TW")

        assertTrue(state.currentTier)
        assertFalse(state.enabled)
        assertEquals("当前方案", state.actionText)
        assertTrue(choices.all { !it.enabled })
    }

    @Test
    fun stripeUpgradeKeepsOnlyStripeChoiceEnabled() {
        val membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_123",
            autoRenew = true,
            status = StripeSubStatus.Active,
        )

        val state = planCheckoutState(premiumProduct, premiumYear, membership, "zh-TW")
        val choices = paymentChoicesForPlan(premiumProduct, premiumYear, membership, "zh-TW")
            .associateBy { it.payMethod }

        assertTrue(state.enabled)
        assertEquals("升级高端会员", state.actionText)
        assertFalse(choices[PayMethod.WXPAY]?.enabled ?: true)
        assertFalse(choices[PayMethod.ALIPAY]?.enabled ?: true)
        assertTrue(choices[PayMethod.STRIPE]?.enabled ?: false)
    }

    @Test
    fun appleActiveSubscriptionDisablesAndroidPurchases() {
        val membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.APPLE,
            appleSubsId = "apple_123",
            autoRenew = true,
        )

        val sameTier = paymentChoicesForPlan(standardProduct, standardYear, membership, "zh-TW")
        val upgrade = paymentChoicesForPlan(premiumProduct, premiumYear, membership, "zh-TW")
        val sameTierState = planCheckoutState(standardProduct, standardYear, membership, "zh-TW")
        val upgradeState = planCheckoutState(premiumProduct, premiumYear, membership, "zh-TW")

        assertTrue(sameTier.all { !it.enabled })
        assertTrue(upgrade.all { !it.enabled })
        assertFalse(upgradeState.enabled)
        assertEquals("当前方案", sameTierState.actionText)
        assertEquals("暂不可购买", upgradeState.actionText)
        assertTrue(upgradeState.message?.contains("苹果") == true)
    }

    @Test
    fun oneTimeCurrentTierCanRenewUntilRenewalLimit() {
        val membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.WXPAY,
            autoRenew = false,
        )

        val state = planCheckoutState(standardProduct, standardYear, membership, "zh-TW")
        val choices = paymentChoicesForPlan(standardProduct, standardYear, membership, "zh-TW")

        assertTrue(state.currentTier)
        assertTrue(state.enabled)
        assertEquals("续订年度订阅", state.actionText)
        assertTrue(choices.all { it.enabled })
    }

    @Test
    fun paymentChoiceMarksOnlyExactCurrentPaymentMethod() {
        val membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.WXPAY,
            autoRenew = false,
        )

        val choices = paymentChoicesForPlan(standardProduct, standardYear, membership, "zh-TW")
            .associateBy { it.payMethod }

        assertTrue(choices[PayMethod.WXPAY]?.current ?: false)
        assertFalse(choices[PayMethod.ALIPAY]?.current ?: true)
        assertFalse(choices[PayMethod.STRIPE]?.current ?: true)
    }

    @Test
    fun activeStripeCurrencyOverridesLanguageForDisplayOption() {
        val membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_gbp",
            autoRenew = true,
            status = StripeSubStatus.Active,
        )
        val summary = SubscriptionCatalogSummary(
            status = "active",
            tier = Tier.STANDARD,
            payMethod = PayMethod.STRIPE,
            expireDate = LocalDate.now().plusMonths(6),
            billingCycle = "year",
            autoRenew = true,
            stripePriceId = "price_standard_gbp",
            stripeCurrency = "gbp",
        )

        val preference = summary.displayPreference(
            membership = membership,
            preferredLanguage = "zh-CN",
        )
        val option = displayOptionForPlan(multiCurrencyStandardYear, preference)
        val choices = paymentChoicesForPlan(
            product = standardProduct.copy(plans = listOf(multiCurrencyStandardYear)),
            plan = multiCurrencyStandardYear,
            membership = membership,
            preferredLanguage = "zh-CN",
            displayPreference = preference,
        )

        assertEquals(CatalogDisplayKind.STRIPE, preference.kind)
        assertEquals("gbp", preference.stripeCurrency)
        assertEquals("standard-year-stripe-gbp", option?.id)
        assertEquals(PayMethod.STRIPE, choices.first().payMethod)
    }

    @Test
    fun activeOneTimePaymentOverridesTaiwanLanguageForDisplayOption() {
        val membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.WXPAY,
            autoRenew = false,
        )
        val summary = SubscriptionCatalogSummary(
            status = "active",
            tier = Tier.STANDARD,
            payMethod = PayMethod.WXPAY,
            expireDate = LocalDate.now().plusMonths(6),
            billingCycle = "year",
            autoRenew = false,
        )

        val preference = summary.displayPreference(
            membership = membership,
            preferredLanguage = "zh-TW",
        )
        val option = displayOptionForPlan(multiCurrencyStandardYear, preference)
        val choices = paymentChoicesForPlan(
            product = standardProduct.copy(plans = listOf(multiCurrencyStandardYear)),
            plan = multiCurrencyStandardYear,
            membership = membership,
            preferredLanguage = "zh-TW",
            displayPreference = preference,
        )

        assertEquals(CatalogDisplayKind.FTC, preference.kind)
        assertEquals("standard-year-ftc", option?.id)
        assertEquals(PayMethod.WXPAY, choices.first().payMethod)
    }

    @Test
    fun languageFallbackAppliesWhenNoActiveSubscription() {
        val membership = Membership()
        val summary = SubscriptionCatalogSummary()

        val preference = summary.displayPreference(
            membership = membership,
            preferredLanguage = "en-GB",
        )
        val option = displayOptionForPlan(multiCurrencyStandardYear, preference)

        assertEquals(CatalogDisplayKind.STRIPE, preference.kind)
        assertEquals("gbp", preference.stripeCurrency)
        assertEquals("standard-year-stripe-gbp", option?.id)
    }

    @Test
    fun oneTimeRenewalLimitBlocksOnlyOneTimeStacking() {
        val membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusYears(4),
            payMethod = PayMethod.WXPAY,
            autoRenew = false,
        )

        val choices = paymentChoicesForPlan(standardProduct, standardYear, membership, "zh-TW")
            .associateBy { it.payMethod }

        assertFalse(choices[PayMethod.WXPAY]?.enabled ?: true)
        assertFalse(choices[PayMethod.ALIPAY]?.enabled ?: true)
        assertTrue(choices[PayMethod.STRIPE]?.enabled ?: false)
    }

    @Test
    fun pendingStripeDowngradeEnablesKeepingCurrentPremium() {
        val membership = Membership(
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
                targetPriceId = "price_standard_year",
                effectiveAt = ZonedDateTime.now().plusMonths(6),
            )
        )

        val premiumState = planCheckoutState(premiumProduct, premiumYear, membership, "zh-TW")
        val standardState = planCheckoutState(standardProduct, standardYear, membership, "zh-TW")
        val premiumChoices = paymentChoicesForPlan(premiumProduct, premiumYear, membership, "zh-TW")
            .associateBy { it.payMethod }
        val standardChoices = paymentChoicesForPlan(standardProduct, standardYear, membership, "zh-TW")
            .associateBy { it.payMethod }

        assertTrue(premiumState.currentTier)
        assertTrue(premiumState.enabled)
        assertEquals("保留高端续订", premiumState.actionText)
        assertEquals(IntentKind.CancelScheduledChange, premiumChoices[PayMethod.STRIPE]?.intentKind)
        assertTrue(premiumChoices[PayMethod.STRIPE]?.isDirectStripeUpdate ?: false)

        assertFalse(standardState.enabled)
        assertEquals("已安排转为标准会员", standardState.actionText)
        assertFalse(standardChoices[PayMethod.STRIPE]?.enabled ?: true)
    }

    @Test
    fun pendingStripeDowngradeCanKeepCurrentPremiumWhenCatalogMarksCurrentOptionDisabled() {
        val membership = Membership(
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
                targetPriceId = "price_standard_year",
                effectiveAt = ZonedDateTime.now().plusMonths(6),
            )
        )
        val disabledPremiumYear = premiumYear.copy(
            options = premiumYear.options.map { option ->
                if (option.kind == "stripe") {
                    option.copy(
                        isActive = true,
                        disabled = true,
                        ctaText = "当前方案",
                    )
                } else {
                    option
                }
            }
        )
        val disabledPremiumProduct = premiumProduct.copy(plans = listOf(disabledPremiumYear))

        val state = planCheckoutState(disabledPremiumProduct, disabledPremiumYear, membership, "zh-TW")
        val stripeChoice = paymentChoicesForPlan(disabledPremiumProduct, disabledPremiumYear, membership, "zh-TW")
            .firstOrNull { it.payMethod == PayMethod.STRIPE }

        assertTrue(state.enabled)
        assertEquals("保留高端续订", state.actionText)
        assertEquals(IntentKind.CancelScheduledChange, stripeChoice?.intentKind)
        assertTrue(stripeChoice?.enabled ?: false)
    }

    @Test
    fun activeStripePendingDowngradeUsesCurrentStripeCurrencyOverLanguage() {
        val fallback = Membership(
            tier = Tier.PREMIUM,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_123",
            autoRenew = true,
            status = StripeSubStatus.Active,
        )
        val summary = SubscriptionCatalogSummary(
            status = "active",
            tier = Tier.PREMIUM,
            payMethod = PayMethod.STRIPE,
            expireDate = LocalDate.now().plusMonths(6),
            billingCycle = "year",
            autoRenew = true,
            stripePriceId = "price_premium_twd",
            stripeCurrency = "twd",
            pendingStripeChange = StripePendingChange(
                kind = "downgrade",
                scheduleId = "sched_123",
                targetTier = Tier.STANDARD,
                targetCycle = Cycle.YEAR,
                targetPriceId = "price_standard_twd",
                effectiveAt = ZonedDateTime.now().plusMonths(6),
            )
        )
        val membership = summary.checkoutMembership(fallback)
        val preference = summary.displayPreference(
            membership = membership,
            preferredLanguage = "en-GB",
        )
        val option = displayOptionForPlan(multiCurrencyPremiumYear, preference)
        val premiumProductWithMultiCurrency = premiumProduct.copy(plans = listOf(multiCurrencyPremiumYear))
        val state = planCheckoutState(
            product = premiumProductWithMultiCurrency,
            plan = multiCurrencyPremiumYear,
            membership = membership,
            preferredLanguage = "en-GB",
            displayPreference = preference,
        )
        val stripeChoice = paymentChoicesForPlan(
            product = premiumProductWithMultiCurrency,
            plan = multiCurrencyPremiumYear,
            membership = membership,
            preferredLanguage = "en-GB",
            displayPreference = preference,
        ).firstOrNull { it.payMethod == PayMethod.STRIPE }

        assertEquals(CatalogDisplayKind.STRIPE, preference.kind)
        assertEquals("twd", preference.stripeCurrency)
        assertEquals("premium-year-stripe-twd", option?.id)
        assertTrue(state.enabled)
        assertEquals("Keep Premium Renewal", state.actionText)
        assertEquals("premium-year-stripe-twd", stripeChoice?.option?.id)
        assertEquals(IntentKind.CancelScheduledChange, stripeChoice?.intentKind)
        assertTrue(stripeChoice?.enabled ?: false)
    }

    @Test
    fun catalogSummaryPendingChangeOverridesLocalFallback() {
        val fallback = Membership(
            tier = Tier.PREMIUM,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_123",
            autoRenew = true,
            status = StripeSubStatus.Active,
        )
        val pendingChange = StripePendingChange(
            kind = "downgrade",
            scheduleId = "sched_123",
            targetTier = Tier.STANDARD,
            targetCycle = Cycle.YEAR,
            targetPriceId = "price_standard_year",
            effectiveAt = ZonedDateTime.now().plusMonths(6),
        )
        val summary = SubscriptionCatalogSummary(
            status = "active",
            tier = Tier.PREMIUM,
            payMethod = PayMethod.STRIPE,
            expireDate = LocalDate.now().plusMonths(6),
            billingCycle = "year",
            autoRenew = true,
            pendingStripeChange = pendingChange,
        )

        val membership = summary.checkoutMembership(fallback)
        val premiumState = planCheckoutState(premiumProduct, premiumYear, membership, "zh-TW")
        val standardState = planCheckoutState(standardProduct, standardYear, membership, "zh-TW")

        assertEquals(pendingChange, membership.pendingStripeChange)
        assertEquals("保留高端续订", premiumState.actionText)
        assertEquals("已安排转为标准会员", standardState.actionText)
        assertFalse(standardState.enabled)
    }

    @Test
    fun standardToPremiumToDowngradeKeepPremiumThenCancelFlowShowsCorrectActions() {
        val standard = stripeMembership(Tier.STANDARD, autoRenew = true)
        assertFlowState(
            membership = standard,
            expectedAutoRenewChecked = true,
            expectedAutoRenewStatus = "标准会员",
            expectedStandardEnabled = false,
            expectedStandardAction = "当前方案",
            expectedPremiumEnabled = true,
            expectedPremiumAction = "升级高端会员",
            expectedPremiumIntent = IntentKind.Upgrade,
        )

        val premium = stripeMembership(Tier.PREMIUM, autoRenew = true)
        assertFlowState(
            membership = premium,
            expectedAutoRenewChecked = true,
            expectedAutoRenewStatus = "高端会员",
            expectedStandardEnabled = true,
            expectedStandardAction = "转为标准会员",
            expectedStandardIntent = IntentKind.Downgrade,
            expectedPremiumEnabled = false,
            expectedPremiumAction = "当前方案",
        )

        val pendingDowngrade = premium.copy(
            pendingStripeChange = StripePendingChange(
                kind = "downgrade",
                scheduleId = "sched_123",
                targetTier = Tier.STANDARD,
                targetCycle = Cycle.YEAR,
                targetPriceId = "price_standard_year",
                effectiveAt = ZonedDateTime.now().plusMonths(6),
            )
        )
        assertFlowState(
            membership = pendingDowngrade,
            expectedAutoRenewChecked = true,
            expectedAutoRenewStatus = "标准会员",
            expectedStandardEnabled = false,
            expectedStandardAction = "已安排转为标准会员",
            expectedPremiumEnabled = true,
            expectedPremiumAction = "保留高端续订",
            expectedPremiumIntent = IntentKind.CancelScheduledChange,
        )

        val keptPremium = premium.copy(pendingStripeChange = null)
        assertFlowState(
            membership = keptPremium,
            expectedAutoRenewChecked = true,
            expectedAutoRenewStatus = "高端会员",
            expectedStandardEnabled = true,
            expectedStandardAction = "转为标准会员",
            expectedStandardIntent = IntentKind.Downgrade,
            expectedPremiumEnabled = false,
            expectedPremiumAction = "当前方案",
        )

        val cancelledPremium = keptPremium.copy(
            autoRenew = false,
            status = StripeSubStatus.Canceled,
        )
        assertFlowState(
            membership = cancelledPremium,
            expectedAutoRenewChecked = false,
            expectedAutoRenewStatus = "已关闭",
            expectedStandardEnabled = false,
            expectedStandardAction = "暂不可购买",
            expectedPremiumEnabled = false,
            expectedPremiumAction = "当前方案",
        )
    }

    private fun assertFlowState(
        membership: Membership,
        expectedAutoRenewChecked: Boolean,
        expectedAutoRenewStatus: String,
        expectedStandardEnabled: Boolean,
        expectedStandardAction: String,
        expectedPremiumEnabled: Boolean,
        expectedPremiumAction: String,
        expectedStandardIntent: IntentKind? = null,
        expectedPremiumIntent: IntentKind? = null,
    ) {
        val autoRenewState = membership.stripeAutoRenewUiState("zh-TW")
        val standardState = planCheckoutState(standardProduct, standardYear, membership, "zh-TW")
        val premiumState = planCheckoutState(premiumProduct, premiumYear, membership, "zh-TW")
        val standardStripe = paymentChoicesForPlan(standardProduct, standardYear, membership, "zh-TW")
            .firstOrNull { it.payMethod == PayMethod.STRIPE }
        val premiumStripe = paymentChoicesForPlan(premiumProduct, premiumYear, membership, "zh-TW")
            .firstOrNull { it.payMethod == PayMethod.STRIPE }

        assertTrue(autoRenewState.visible)
        assertEquals(expectedAutoRenewChecked, autoRenewState.checked)
        assertTrue(autoRenewState.status.contains(expectedAutoRenewStatus))
        assertEquals(expectedStandardEnabled, standardState.enabled)
        assertEquals(expectedStandardAction, standardState.actionText)
        assertEquals(expectedPremiumEnabled, premiumState.enabled)
        assertEquals(expectedPremiumAction, premiumState.actionText)
        expectedStandardIntent?.let {
            assertEquals(it, standardStripe?.intentKind)
        }
        expectedPremiumIntent?.let {
            assertEquals(it, premiumStripe?.intentKind)
        }
    }

    private fun stripeMembership(
        tier: Tier,
        autoRenew: Boolean,
    ): Membership {
        return Membership(
            tier = tier,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_123",
            autoRenew = autoRenew,
            status = if (autoRenew) StripeSubStatus.Active else StripeSubStatus.Canceled,
        )
    }

    private companion object {
        val standardYear = yearlyPlan(
            id = "standard-year",
            title = "年度订阅",
            ftcPriceId = "standard_year_cny",
            stripePriceId = "price_standard_year"
        )

        val premiumYear = yearlyPlan(
            id = "premium-year",
            title = "年度订阅",
            ftcPriceId = "premium_year_cny",
            stripePriceId = "price_premium_year"
        )

        val multiCurrencyStandardYear = SubscriptionCatalogPlan(
            id = "standard-year",
            cycle = "year",
            title = "年度订阅",
            options = listOf(
                SubscriptionCatalogOption(
                    id = "standard-year-ftc",
                    kind = "ftc",
                    displayPrice = "¥268.00 / 年",
                    originalPrice = "¥358.00 / 年",
                    checkout = SubscriptionCheckout(
                        ftcPriceId = "standard_year_cny"
                    )
                ),
                SubscriptionCatalogOption(
                    id = "standard-year-stripe-twd",
                    kind = "stripe",
                    displayPrice = "NT$1117.50 / 年",
                    originalPrice = "NT$1490.00 / 年",
                    checkout = SubscriptionCheckout(
                        stripePriceId = "price_standard_twd",
                        stripeCurrency = "twd",
                        stripeUnitAmount = 149000,
                        stripePayableAmount = 111750,
                        stripeCouponAmountOff = 37250,
                        stripeCouponId = "coupon_twd"
                    )
                ),
                SubscriptionCatalogOption(
                    id = "standard-year-stripe-gbp",
                    kind = "stripe",
                    displayPrice = "£29.25 / 年",
                    originalPrice = "£39.00 / 年",
                    checkout = SubscriptionCheckout(
                        stripePriceId = "price_standard_gbp",
                        stripeCurrency = "gbp",
                        stripeUnitAmount = 3900,
                        stripePayableAmount = 2925,
                        stripeCouponAmountOff = 975,
                        stripeCouponId = "coupon_gbp"
                    )
                ),
            )
        )

        val multiCurrencyPremiumYear = SubscriptionCatalogPlan(
            id = "premium-year",
            cycle = "year",
            title = "年度订阅",
            options = listOf(
                SubscriptionCatalogOption(
                    id = "premium-year-ftc",
                    kind = "ftc",
                    displayPrice = "¥1268.50 / 年",
                    originalPrice = "¥1698.00 / 年",
                    checkout = SubscriptionCheckout(
                        ftcPriceId = "premium_year_cny"
                    )
                ),
                SubscriptionCatalogOption(
                    id = "premium-year-stripe-gbp",
                    kind = "stripe",
                    displayPrice = "£166.50 / 年",
                    originalPrice = "£222.00 / 年",
                    checkout = SubscriptionCheckout(
                        stripePriceId = "price_premium_gbp",
                        stripeCurrency = "gbp",
                        stripeUnitAmount = 22200,
                        stripePayableAmount = 16650,
                        stripeCouponAmountOff = 5550,
                        stripeCouponId = "coupon_gbp"
                    )
                ),
                SubscriptionCatalogOption(
                    id = "premium-year-stripe-twd",
                    kind = "stripe",
                    displayPrice = "NT$6375.00 / 年",
                    originalPrice = "NT$8500.00 / 年",
                    checkout = SubscriptionCheckout(
                        stripePriceId = "price_premium_twd",
                        stripeCurrency = "twd",
                        stripeUnitAmount = 850000,
                        stripePayableAmount = 637500,
                        stripeCouponAmountOff = 212500,
                        stripeCouponId = "coupon_twd"
                    )
                ),
            )
        )

        fun yearlyPlan(
            id: String,
            title: String,
            ftcPriceId: String,
            stripePriceId: String,
        ): SubscriptionCatalogPlan {
            return SubscriptionCatalogPlan(
                id = id,
                cycle = "year",
                title = title,
                options = listOf(
                    SubscriptionCatalogOption(
                        id = "${id}_ftc",
                        kind = "ftc",
                        displayPrice = "¥268.00 / 年",
                        originalPrice = "¥358.00 / 年",
                        checkout = SubscriptionCheckout(
                            ftcPriceId = ftcPriceId
                        )
                    ),
                    SubscriptionCatalogOption(
                        id = "${id}_stripe",
                        kind = "stripe",
                        displayPrice = "NT$1117.50 / 年",
                        originalPrice = "NT$1490.00 / 年",
                        checkout = SubscriptionCheckout(
                            stripePriceId = stripePriceId,
                            stripeCurrency = "twd",
                            stripeUnitAmount = 149000,
                            stripePayableAmount = 111750,
                            stripeCouponAmountOff = 37250,
                            stripeCouponId = "coupon_twd"
                        )
                    )
                )
            )
        }
    }
}
