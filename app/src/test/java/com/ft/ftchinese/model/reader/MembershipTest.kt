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
    }

}
