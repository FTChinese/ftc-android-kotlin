package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.OfferKind
import com.ft.ftchinese.model.enums.PriceKind
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.Discount
import com.ft.ftchinese.model.enums.DiscountStatus
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.ftcsubs.YearMonthDay

/**
 * A hard-coded paywall data used when server is not available.
 */
val defaultPaywall = Paywall(
    id = 2,
    banner = Banner(
        id = "banner_m22KbtGLRP90",
        heading = "FT中文网会员订阅服务",
        subHeading = "欢迎您！",
        coverUrl = "http://www.ftacademy.cn/subscription.jpg",
        content = "希望全球视野的FT中文网，能够带您站在高海拔的地方俯瞰世界，引发您的思考，从不同的角度看到不一样的事物，见他人之未见！",
        startUtc = null,
        endUtc = null
    ),
    promo = Banner(
        id = "",
        heading = "",
        subHeading = null,
        coverUrl = null,
        content = null,
        terms = null,
        startUtc = null,
        endUtc = null
    ),
    products = listOf(
        PaywallProduct(
            id = "prod_9xrJdHFq0wmq",
            tier = Tier.STANDARD,
            heading = "标准会员",
            description = "专享订阅内容每日仅需{{dailyAverageOfYear}}元(或按月订阅每日{{dailyAverageOfMonth}}元)\n精选深度分析\n中英双语内容\n金融英语速读训练\n英语电台\n阅读1日前历史文章（近9万篇）",
            smallPrint = null,
            introductory = null,
            prices = listOf(
                Price(
                    id = "plan_RKy1IuKSXyua",
                    tier = Tier.STANDARD,
                    cycle = Cycle.YEAR,
                    active = true,
                    currency = "cny",
                    kind = PriceKind.Recurring,
                    liveMode = true,
                    nickname = "标准会员",
                    periodCount = YearMonthDay(
                        years = 1,
                        months = 0,
                        days = 0,
                    ),
                    productId = "prod_9xrJdHFq0wmq",
                    stripePriceId = "",
                    unitAmount = 298.0,
                    startUtc = null,
                    endUtc = null,
                    offers = listOf(
                        Discount(
                            id = "dsc_fAFaUX9VreYX",
                            currency = "cny",
                            description =  "现在续订享75折优惠",
                            kind = OfferKind.Retention,
                            startUtc = null,
                            endUtc = null,
                            priceOff = 80.0,
                            percent = null,
                            priceId = "plan_RKy1IuKSXyua",
                            recurring = true,
                            liveMode = true,
                            status = DiscountStatus.Active,
                        ),
                        Discount(
                            id = "dsc_2XOFwX42bAvI",
                            currency = "cny",
                            description = "重新购买会员享85折优惠",
                            kind = OfferKind.WinBack,
                            startUtc = null,
                            endUtc = null,
                            priceOff = 40.0,
                            percent = null,
                            priceId = "plan_RKy1IuKSXyua",
                            recurring = true,
                            liveMode = true,
                            status = DiscountStatus.Active,
                        )
                    ),
                ),
                Price(
                    id = "plan_ohky3lyEMPSf",
                    tier = Tier.STANDARD,
                    cycle = Cycle.MONTH,
                    active = true,
                    currency = "cny",
                    kind = PriceKind.Recurring,
                    liveMode = true,
                    nickname = "标准会员",
                    periodCount = YearMonthDay(
                        years = 0,
                        months = 1,
                        days = 0,
                    ),
                    productId = "prod_9xrJdHFq0wmq",
                    stripePriceId = "",
                    unitAmount = 35.0,
                    startUtc = null,
                    endUtc = null,
                    offers = listOf()
                ),
            )
        ),
        PaywallProduct(
            id = "prod_zSgOTS6DWLmu",
            tier = Tier.PREMIUM,
            heading = "高端会员",
            description = "专享订阅内容每日仅需{{dailyAverageOfYear}}元\n享受“标准会员”所有权益\n编辑精选，总编/各版块主编每周五为您推荐本周必读资讯，分享他们的思考与观点\nFT商学院高端专享\nFT中文网2022年度论坛门票2张",
            smallPrint = "注：所有活动门票不可折算现金、不能转让、不含差旅与食宿",
            introductory = null,
            prices = listOf(
                Price(
                    id = "plan_rLIy6LJYW8LV",
                    tier = Tier.PREMIUM,
                    cycle = Cycle.YEAR,
                    active = true,
                    currency = "cny",
                    kind = PriceKind.Recurring,
                    liveMode = true,
                    nickname = "Premium Yearly Edition",
                    periodCount = YearMonthDay(
                        years = 1,
                        months = 0,
                        days = 0,
                    ),
                    productId = "prod_zSgOTS6DWLmu",
                    stripePriceId = "",
                    unitAmount =  1998.0,
                    startUtc = null,
                    endUtc = null,
                    offers = listOf(
                        Discount(
                            id = "dsc_4jvMFdhEYXWk",
                            currency = "cny",
                            description = "现在续订享75折优惠",
                            kind = OfferKind.Retention,
                            startUtc = null,
                            endUtc = null,
                            priceOff = 500.0,
                            percent = null,
                            priceId = "plan_rLIy6LJYW8LV",
                            recurring = true,
                            liveMode = true,
                            status = DiscountStatus.Active,
                        ),
                        Discount(
                            id = "dsc_GzsiHfNQuKyn",
                            currency = "cny",
                            description = "重新购买会员享85折优惠",
                            kind = OfferKind.WinBack,
                            startUtc = null,
                            endUtc = null,
                            priceOff = 300.0,
                            percent = null,
                            recurring = true,
                            liveMode = true,
                            status = DiscountStatus.Active
                        )
                    ),
                ),
            ),
        ),
    ),
)
