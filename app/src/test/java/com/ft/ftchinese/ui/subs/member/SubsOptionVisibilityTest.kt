package com.ft.ftchinese.ui.subs.member

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.StripeSubStatus
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePendingChange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime

class SubsOptionVisibilityTest {

    @Test
    fun stripeAutoRenewingShowsCheckedSwitch() {
        val visibility = stripeMembership(
            autoRenew = true,
            expireDate = LocalDate.now().plusMonths(6),
        ).subsOptionVisibility()

        assertTrue(visibility.showStripeAutoRenewSwitch)
        assertTrue(visibility.stripeAutoRenewChecked)
    }

    @Test
    fun cancelledStripeShowsUncheckedSwitchBeforeExpiration() {
        val visibility = stripeMembership(
            autoRenew = false,
            expireDate = LocalDate.now().plusMonths(6),
        ).subsOptionVisibility()

        assertTrue(visibility.showStripeAutoRenewSwitch)
        assertFalse(visibility.stripeAutoRenewChecked)
    }

    @Test
    fun expiredStripeShowsNoAutoRenewActions() {
        val visibility = stripeMembership(
            autoRenew = false,
            expireDate = LocalDate.now().minusDays(1),
        ).subsOptionVisibility()

        assertFalse(visibility.showStripeAutoRenewSwitch)
        assertFalse(visibility.stripeAutoRenewChecked)
    }

    @Test
    fun oneTimePaymentsShowNoAutoRenewActions() {
        for (payMethod in listOf(PayMethod.WXPAY, PayMethod.ALIPAY)) {
            val visibility = Membership(
                tier = Tier.STANDARD,
                cycle = Cycle.YEAR,
                expireDate = LocalDate.now().plusMonths(6),
                payMethod = payMethod,
                autoRenew = false,
            ).subsOptionVisibility()

            assertFalse(visibility.showStripeAutoRenewSwitch)
            assertFalse(visibility.stripeAutoRenewChecked)
        }
    }

    @Test
    fun appleSubscriptionIsNotManagedByAndroidStripeControls() {
        val visibility = Membership(
            tier = Tier.PREMIUM,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.APPLE,
            appleSubsId = "apple_123",
            autoRenew = true,
        ).subsOptionVisibility()

        assertFalse(visibility.showStripeAutoRenewSwitch)
        assertFalse(visibility.stripeAutoRenewChecked)
    }

    @Test
    fun stripeActionsRequireStripeSubscriptionId() {
        val visibility = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.STRIPE,
            autoRenew = true,
            status = StripeSubStatus.Active,
        ).subsOptionVisibility()

        assertFalse(visibility.showStripeAutoRenewSwitch)
        assertFalse(visibility.stripeAutoRenewChecked)
    }

    @Test
    fun invalidStripeStatusShowsNoAutoRenewActions() {
        val visibility = stripeMembership(
            autoRenew = true,
            expireDate = LocalDate.now().plusMonths(6),
            status = StripeSubStatus.PastDue,
        ).subsOptionVisibility()

        assertFalse(visibility.showStripeAutoRenewSwitch)
        assertFalse(visibility.stripeAutoRenewChecked)
    }

    @Test
    fun pendingDowngradeShowsAutoRenewOnWithNextTier() {
        val visibility = Membership(
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
            ),
        ).subsOptionVisibility()

        assertTrue(visibility.showStripeAutoRenewSwitch)
        assertTrue(visibility.stripeAutoRenewChecked)
        assertTrue(visibility.stripeAutoRenewUiState?.status?.contains("标准会员") == true)
        assertTrue(visibility.stripeAutoRenewUiState?.offConfirmation?.contains("取消已安排的下次降级") == true)
    }

    private fun stripeMembership(
        autoRenew: Boolean,
        expireDate: LocalDate,
        status: StripeSubStatus = if (autoRenew) StripeSubStatus.Active else StripeSubStatus.Canceled,
    ): Membership {
        return Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = expireDate,
            payMethod = PayMethod.STRIPE,
            stripeSubsId = "sub_123",
            autoRenew = autoRenew,
            status = status,
        )
    }
}
