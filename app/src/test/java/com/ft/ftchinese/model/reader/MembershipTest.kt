package com.ft.ftchinese.model.reader

import com.ft.ftchinese.model.NextStep
import com.ft.ftchinese.model.Permission
import com.ft.ftchinese.model.order.Cycle
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.model.order.StripeSubStatus
import com.ft.ftchinese.model.order.Tier
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

    @Test fun remainingDays() {
        val diff = LocalDate.now().until(LocalDate.of(2020, Month.JULY, 4), ChronoUnit.DAYS)

        println(diff)
    }

    @Test fun vip() {

        val (readerPermBits, status) = mVip.getPermission()
        val nextAction = mVip.nextAction()

        println("Member status $status")

        assertTrue(
                "VIP can read free content",
                (readerPermBits and Permission.FREE.id) > 0
        )
        assertTrue(
                "VIP can read standard content",
                (readerPermBits and Permission.STANDARD.id) > 0
        )
        assertTrue(
                "VIP can read premium content",
                (readerPermBits and Permission.PREMIUM.id) > 0
        )

        assertTrue("VIP has no next action", nextAction == NextStep.None.id)
    }

    @Test fun standard() {

        val (readerPermBits, status) = mStandard.getPermission()
        val nextAction = mStandard.nextAction()

        println("Member status $status")

        assertTrue(
                "Standard member can read free content",
                (readerPermBits and Permission.FREE.id) > 0
        )
        assertTrue(
                "Standard member can read standard content",
                (readerPermBits and Permission.STANDARD.id) > 0
        )
        assertFalse(
                "Standard member cannot reader premium content",
                (readerPermBits and Permission.PREMIUM.id) > 0
        )

        assertTrue(
                "Standard member can renew",
                (nextAction and NextStep.Renew.id) > 0
        )

        assertTrue(
                "Standard member can upgrade",
                (nextAction and NextStep.Upgrade.id) > 0
        )
    }

    @Test fun premium() {


        val (readerPermBits, status) = mPremium.getPermission()

        println("Member status $status")

        assertTrue((readerPermBits and Permission.FREE.id) > 0)
        assertTrue((readerPermBits and Permission.STANDARD.id) > 0)
        assertTrue((readerPermBits and Permission.PREMIUM.id) > 0)
    }

    @Test fun emptyPerm() {
        val m = Membership()

        val (readerPermBits, status) = m.getPermission()

        println("Member status $status")

        assertTrue((readerPermBits and Permission.FREE.id) > 0)
        assertTrue((readerPermBits and Permission.STANDARD.id) == 0)
        assertTrue((readerPermBits and Permission.PREMIUM.id) == 0)
    }

    @Test fun stripeInactive() {
        val m = Membership(
                tier = Tier.STANDARD,
                payMethod = PayMethod.STRIPE,
                status = StripeSubStatus.Incomplete
        )

        val (readerPermBits, status) = m.getPermission()

        println("Member status $status")

        assertTrue((readerPermBits and Permission.FREE.id) > 0)
        assertTrue((readerPermBits and Permission.STANDARD.id) == 0)
        assertTrue((readerPermBits and Permission.PREMIUM.id) == 0)
    }

    @Test fun expiredStandard() {
        val m = Membership(
                tier = Tier.STANDARD,
                expireDate = LocalDate.now().plusMonths(-1)
        )

        val (readerPermBits, status) = m.getPermission()

        println("Member status $status")

        assertTrue((readerPermBits and Permission.FREE.id) > 0)
        assertTrue((readerPermBits and Permission.STANDARD.id) == 0)
        assertTrue((readerPermBits and Permission.PREMIUM.id) == 0)
    }

    @Test fun expiredPremium() {
        val m = Membership(
                tier = Tier.PREMIUM,
                expireDate = LocalDate.now().plusMonths(-1)
        )

        val (readerPermBits, status) = m.getPermission()

        println("Member status $status")

        assertTrue((readerPermBits and Permission.FREE.id) > 0)
        assertTrue((readerPermBits and Permission.STANDARD.id) == 0)
        assertTrue((readerPermBits and Permission.PREMIUM.id) == 0)
    }
}
