package com.ft.ftchinese.model.reader

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.StripeSubStatus
import com.ft.ftchinese.model.enums.Tier
import org.junit.Assert.*
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.temporal.ChronoUnit

class MembershipTest {

    private val jsonData = """
    {
        "tier": "premium",
        "cycle": "year",
        "expireDate": "2021-12-11",
        "payMethod": "stripe",
        "stripeSubsId": "sub_IY75arTimVigIr",
        "autoRenew": true,
        "status": "active",
        "appleSubsId": null,
        "b2bLicenceId": null,
        "vip": false
    }
    """.trimIndent()

    private val mVip =  Membership(
            tier = null,
            cycle = null,
            expireDate = null,
            payMethod = null,
            autoRenew = false,
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

    private val stripeWithAddOn = Membership(
        tier = Tier.STANDARD,
        cycle = Cycle.YEAR,
        expireDate = LocalDate.now().plusMonths(-1),
        payMethod = PayMethod.STRIPE,
        autoRenew = true,
        status = StripeSubStatus.Canceled,
        vip = false,
        standardAddOn = 30,
        premiumAddOn = 366
    )

    @Test fun remainingDays() {
        val diff = LocalDate.now().until(LocalDate.of(2020, Month.JULY, 4), ChronoUnit.DAYS)

        println(diff)
    }

    @Test fun vip() {

        val (readerPermBits, status) = mVip.accessRights()

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

        val (readerPermBits, status) = mStandard.accessRights()

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

        val (readerPermBits, status) = mPremium.accessRights()


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

        val (readerPermBits, status) = m.accessRights()

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

        val (readerPermBits, status) = mStandardExpired.accessRights()

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

        val (readerPermBits, status) = mPremiumExpired.accessRights()

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
        val (readerPermBits, _) = mStandardStripe.accessRights()

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
        val (readerPermBits, _) = mPremiumStripe.accessRights()

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
        val (readerPermBits, _) = mStripeIncomplete.accessRights()

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
        val (readerPermBits, _) = mStripeInactive.accessRights()

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

    @Test fun addOn() {
        val hasStdAddOn = stripeWithAddOn.hasStandardAddOn
        val hasPrmAddOn = stripeWithAddOn.hasPremiumAddOn

        assertTrue(hasStdAddOn)
        assertTrue(hasPrmAddOn)

        val addOns = mutableListOf<Pair<String, String>>().apply {
            if (stripeWithAddOn.hasStandardAddOn) {
                add(Pair("标准版AddOn", "${stripeWithAddOn.standardAddOn}天"))
            }
            if (stripeWithAddOn.hasPremiumAddOn) {
                add(Pair("高端版AddOn", "${stripeWithAddOn.premiumAddOn}天"))
            }
        }

        val details = mutableListOf(
            Pair("订阅方式", "Stripe"),
            Pair("期限", stripeWithAddOn.localizeExpireDate()),
            Pair("自动续订", "已关闭"),
        ).apply {
            addAll(addOns)
        }
        println(details)
    }
}
