package com.ft.ftchinese.model.reader

import com.ft.ftchinese.model.Permission
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.model.order.StripeSubStatus
import com.ft.ftchinese.model.order.Tier
import org.junit.Assert.*
import org.junit.Test
import org.threeten.bp.LocalDate

class MembershipTest {
    @Test fun vip() {
        val m = Membership(
                vip = true
        )
        val (readerPermBits, status) = m.getPermission()

        println("Member status $status")

        assertTrue((readerPermBits and Permission.FREE.id) > 0)
        assertTrue((readerPermBits and Permission.STANDARD.id) > 0)
        assertTrue((readerPermBits and Permission.PREMIUM.id) > 0)
    }

    @Test fun standard() {
        val m = Membership(
                expireDate = LocalDate.now().plusMonths(1),
                tier = Tier.STANDARD
        )

        val (readerPermBits, status) = m.getPermission()

        println("Member status $status")

        assertTrue((readerPermBits and Permission.FREE.id) > 0)
        assertTrue((readerPermBits and Permission.STANDARD.id) > 0)
        assertTrue((readerPermBits and Permission.PREMIUM.id) == 0)
    }

    @Test fun premium() {
        val m = Membership(
                expireDate = LocalDate.now().plusMonths(1),
                tier = Tier.PREMIUM
        )

        val (readerPermBits, status) = m.getPermission()

        println("Member status $status")

        assertTrue((readerPermBits and Permission.FREE.id) > 0)
        assertTrue((readerPermBits and Permission.STANDARD.id) > 0)
        assertTrue((readerPermBits and Permission.PREMIUM.id) > 0)
    }

    @Test fun empty() {
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
