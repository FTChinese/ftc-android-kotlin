package com.ft.ftchinese.model.reader

import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.PayMethod
import com.ft.ftchinese.model.order.StripeSubStatus
import com.ft.ftchinese.model.subscription.Tier
import org.junit.Assert.*
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.temporal.ChronoUnit

class MembershipTest {

    private val mVip =  Membership(
            tier = null,
            cycle = null,
            expireDate = null,
            payMethod = null,
            autoRenew = null,
            status = null,
            vip = true
    )

    private val mStandard = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(1),
            payMethod = PayMethod.ALIPAY,
            autoRenew = false,
            status = null,
            vip = false
    )

    private val mPremium = Membership(
            tier = Tier.PREMIUM,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(1),
            payMethod = PayMethod.WXPAY,
            autoRenew = false,
            status = null,
            vip = false
    )

    private val mStandardBeyondRenewal = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusYears(4),
            payMethod = PayMethod.WXPAY,
            autoRenew = false,
            status = null,
            vip = false
    )

    private val mPremiumBeyondRenewal = Membership(
            tier = Tier.PREMIUM,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusYears(4),
            payMethod = PayMethod.ALIPAY,
            autoRenew = false,
            status = null,
            vip = false
    )

    private val mStandardExpired = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(-1),
            payMethod = PayMethod.ALIPAY,
            autoRenew = false,
            status = null,
            vip = false
    )

    private val mPremiumExpired = Membership(
            tier = Tier.PREMIUM,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(-1),
            payMethod = PayMethod.WXPAY,
            autoRenew = false,
            status = null,
            vip = false
    )

    private val mStandardStripe = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusYears(1),
            payMethod = PayMethod.STRIPE,
            autoRenew = true,
            status = StripeSubStatus.Active,
            vip = false
    )

    private val mPremiumStripe = Membership(
            tier = Tier.PREMIUM,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusYears(1),
            payMethod = PayMethod.STRIPE,
            autoRenew = true,
            status = StripeSubStatus.Active,
            vip = false
    )

    private val mStripeIncomplete = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusYears(1),
            payMethod = PayMethod.STRIPE,
            autoRenew = true,
            status = StripeSubStatus.Incomplete,
            vip = false
    )

    private val mStripeInactive = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(1),
            payMethod = PayMethod.STRIPE,
            autoRenew = true,
            status = StripeSubStatus.IncompleteExpired,
            vip = false
    )

    @Test fun remainingDays() {
        val diff = LocalDate.now().until(LocalDate.of(2020, Month.JULY, 4), ChronoUnit.DAYS)

        println(diff)
    }

    @Test fun vip() {

        val (readerPermBits, status) = mVip.getPermission()

        println("Member status $status")

        assertTrue(
                "VIP can read free content",
                Permission.FREE.grant(readerPermBits)
        )
        assertTrue(
                "VIP can read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )
        assertTrue(
                "VIP can read premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )

        val buttons = mVip.nextVisibleButtons()
        assertFalse(
                "VIP does not show subscribe button",
                buttons.showSubscribe
        )
        assertFalse(
                "VIP does not show renew buttons",
                buttons.showRenew
        )
        assertFalse(
                "VIP does not show upgrade buttons",
                buttons.showUpgrade
        )
    }

    @Test fun standard() {

        val (readerPermBits, status) = mStandard.getPermission()

        println("Member status $status")

        assertTrue(
                "Standard member can read free content",
                Permission.FREE.grant(readerPermBits)
        )
        assertTrue(
                "Standard member can read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )
        assertFalse(
                "Standard member cannot reader premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )

        val buttons = mStandard.nextVisibleButtons()
        assertFalse(
                "Standard member does not show subscribe button",
                buttons.showSubscribe
        )
        assertTrue(
                "Standard member show renew button",
                buttons.showRenew
        )
        assertTrue(
                "Standard member show upgrade button",
                buttons.showUpgrade
        )
    }

    @Test fun premium() {

        val (readerPermBits, status) = mPremium.getPermission()


        println("Member status $status")

        assertTrue(
                "Premium member can read free content",
                Permission.FREE.grant(readerPermBits)
        )
        assertTrue(
                "Premium member can read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )
        assertTrue(
                "Premium member can reader premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )

        val buttons = mPremium.nextVisibleButtons()
        // Buttons
        assertFalse(
            "Valid premium member does not re-subscribe button",
                buttons.showSubscribe
        )

        assertTrue(
                "Valid premium member show renew button",
                buttons.showRenew
        )

        assertFalse(
                "Valid premium member does not show upgrade button",
                buttons.showUpgrade
        )
    }

    @Test fun empty() {
        val m = Membership()

        val (readerPermBits, status) = m.getPermission()

        println("Member status $status")

        assertTrue(
                "Non-login user can read free content",
                Permission.FREE.grant(readerPermBits)
        )
        assertFalse(
                "Non-login user cannot read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )
        assertFalse(
                "Non-login user cannot reader premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )
    }


    @Test fun standardExpired() {

        val (readerPermBits, status) = mStandardExpired.getPermission()

        println("Member status $status")

        assertTrue(
                "Expired standard can read free content",
                Permission.FREE.grant(readerPermBits)
        )
        assertFalse(
                "Expired standard cannot read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )
        assertFalse(
                "Standard member can never reader premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )

        val buttons = mStandardExpired.nextVisibleButtons()

        assertTrue(
                "Expired standard show subscribe button",
                buttons.showSubscribe
        )

        assertFalse(
                "Expired standard does not show renew button",
                buttons.showRenew
        )

        assertFalse(
                "Expired standard does not show upgrade button",
                buttons.showUpgrade
        )
    }

    @Test fun premiumExpired() {

        val (readerPermBits, status) = mPremiumExpired.getPermission()

        println("Member status $status")

        assertTrue(
                "Expired premium can read free content",
                Permission.FREE.grant(readerPermBits)
        )
        assertFalse(
                "Expired premium cannot read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )
        assertFalse(
                "Expired premium cannot read premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )

        // Visible buttons
        val buttons = mPremiumExpired.nextVisibleButtons()
        assertTrue(
                "Expired premium show subscribe button",
                buttons.showSubscribe
        )

        assertFalse(
                "Expired premium does not show renew button",
                buttons.showRenew
        )

        assertFalse(
                "Expired premium does not show upgrade button",
                buttons.showUpgrade
        )
    }

    @Test fun standardBeyondRenewal() {
        val buttons = mStandardBeyondRenewal.nextVisibleButtons()

        assertFalse(
                "Standard beyond renewal does not show subscribe button",
                buttons.showSubscribe

        )

        assertFalse(
                "Standard beyond renewal does not show renewal button",
                buttons.showRenew
        )

        assertTrue(
                "Standard beyond renewal show upgrade button",
                buttons.showUpgrade
        )
    }

    @Test fun premiumBeyondRenewal() {
        val buttons = mPremiumBeyondRenewal.nextVisibleButtons()

        assertFalse(
                "Premium beyond renewal does not show subscribe button",
                buttons.showSubscribe

        )

        assertFalse(
                "Premium beyond renewal does not show renewal button",
                buttons.showRenew
        )

        assertFalse(
                "Premium beyond renewal does not show upgrade button",
                buttons.showUpgrade
        )
    }

    @Test fun stripeStandard() {
        val (readerPermBits, status) = mStandardStripe.getPermission()

        assertTrue(
                "Active stripe standard can read free content",
                Permission.FREE.grant(readerPermBits)
        )

        assertTrue(
                "Active stripe standard can read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )
        assertFalse(
                "Active stripe standard cannot reade premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )

        val buttons = mStandardStripe.nextVisibleButtons()
        assertFalse(
                "Standard stripe does not show subscribe button",
                buttons.showSubscribe
        )
        assertFalse(
                "Standard stripe does not show renew button",
                buttons.showRenew
        )
        assertTrue(
                "Standard stripe show upgrade button",
                buttons.showUpgrade
        )
    }

    @Test fun stripePremium() {
        val (readerPermBits, status) = mPremiumStripe.getPermission()

        assertTrue(
                "Active stripe premium can read free content",
                Permission.FREE.grant(readerPermBits)
        )

        assertTrue(
                "Active stripe premium can read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )
        assertTrue(
                "Active stripe premium cannot read premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )

        val buttons = mPremiumStripe.nextVisibleButtons()
        assertFalse(
                "Premium stripe does not show subscribe button",
                buttons.showSubscribe
        )
        assertFalse(
                "Premium stripe does not show renew button",
                buttons.showRenew
        )
        assertFalse(
                "Premium stripe does not show upgrade",
                buttons.showUpgrade
        )
    }

    @Test fun stripeIncomplete() {
        val (readerPermBits, status) = mStripeIncomplete.getPermission()

        assertTrue(
                "Incomplete stripe can read free content",
                Permission.FREE.grant(readerPermBits)
        )

        assertFalse(
                "Incomplete stripe cannot read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )

        assertFalse(
                "Incomplete stripe cannot read premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )

        val buttons = mStripeIncomplete.nextVisibleButtons()
        assertFalse(
                "Incomplete stripe does not show subscribe button",
                buttons.showSubscribe
        )
        assertFalse(
                "Incomplete stripe does not show renew button",
                buttons.showRenew
        )
        assertTrue(
                "Incomplete stripe show upgrade button",
                buttons.showUpgrade
        )
    }

    @Test fun stripeInactive() {
        val (readerPermBits, status) = mStripeInactive.getPermission()

        assertTrue(
                "Inactive stripe can read free content",
                Permission.FREE.grant(readerPermBits)
        )

        assertFalse(
                "Inactive stripe cannot read standard content",
                Permission.STANDARD.grant(readerPermBits)
        )
        assertFalse(
                "Inactive stripe cannot read premium content",
                Permission.PREMIUM.grant(readerPermBits)
        )

        // Such user could only re-subscribe.
        val buttons = mStripeInactive.nextVisibleButtons()
        assertTrue(
                "Inactive stripe show subscribe button",
                buttons.showSubscribe
        )
        assertFalse(
                "Inactive stripe does not show renew button",
                buttons.showRenew
        )
        assertFalse(
                "Inactive strip does not show upgrade button",
                buttons.showUpgrade
        )
    }

}
