package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KDateTime
import com.ft.ftchinese.model.fetch.KTier
import org.threeten.bp.ZonedDateTime

/**
 * Defines the data to present product on paywall.
 * By default the data is hard-coded in string resources.
 */
data class Product(
        val id: String,
        @KTier
    val tier: Tier,
        val heading: String,
        val description: String?,
        val smallPrint: String?,
        val plans: List<Plan>
)

data class Banner(
    val id: Int,
    val heading: String,
    val subHeading: String? = null,
    val coverUrl: String? = null,
    val content: String? = null
)

data class Promo(
    val id: String? = null,
    val heading: String? = null,
    val subHeading: String? = null,
    val coverUrl: String? = null,
    val content: String? = null,
    val terms: String? = null,
    @KDateTime
    val startUtc: ZonedDateTime?,
    @KDateTime
    val endUtc: ZonedDateTime?
) {
    fun isValid(): Boolean {
        if (id == null) {
            return false
        }

        val now = ZonedDateTime.now()

        return !now.isBefore(startUtc) && !now.isAfter(endUtc)
    }
}

data class Paywall(
    val banner: Banner,
    val promo: Promo,
    val products: List<Product>
)

/**
 * A hard-coded paywall data used when server is not available.
 */
val defaultPaywall = Paywall(
    banner = Banner(
        id = 1,
        heading = "FT中文网会员订阅服务",
        subHeading = "欢迎您！",
        coverUrl = "http://www.ftacademy.cn/subscription.jpg",
        content = "希望全球视野的FT中文网，能够带您站在高海拔的地方俯瞰世界，引发您的思考，从不同的角度看到不一样的事物，见他人之未见！",
    ),
    promo = Promo(
        id = null,
        heading = null,
        subHeading = null,
        coverUrl = null,
        content = null,
        terms = null,
        startUtc = null,
        endUtc = null
    ),
    products = listOf(
        Product(
            id = "prod_9xrJdHFq0wmq",
            tier = Tier.STANDARD,
            heading = "标准会员",
            description = "专享订阅内容每日仅需0.72元(或按月订阅每日0.93元)\n精选深度分析\n中英双语内容\n金融英语速读训练\n英语原声电台\n无限浏览2日前所有历史文章（近9万篇）",
            smallPrint = null,
            plans = listOf(
                Plan(
                    id = "plan_CvvaJr9jyGYt",
                    productId = "prod_9xrJdHFq0wmq",
                    price = 258.0,
                    tier = Tier.STANDARD,
                    cycle = Cycle.YEAR,
                    description = "Standard Yearly Plan",
                    discount = Discount(
                        id = "dsc_GWefTkSAqT2m",
                        priceOff = 40.0,
                        startUtc = ZonedDateTime.parse("2021-02-01T04:00:00Z"),
                        endUtc =  ZonedDateTime.parse("2021-02-07T16:00:00Z"),
                    )
                ),
                Plan(
                    id = "plan_8laAatSmDeGd",
                    productId = "prod_9xrJdHFq0wmq",
                    price = 28.0,
                    tier = Tier.STANDARD,
                    cycle = Cycle.MONTH,
                    description = "Standard Monthly Plan",
                    discount = Discount()
                ),
            )
        ),
        Product(
            id = "prod_zSgOTS6DWLmu",
            tier = Tier.PREMIUM,
            heading = "高端会员",
            description = "专享订阅内容每日仅需5.5元\n享受“标准会员”所有权益\n编辑精选，总编/各版块主编每周五为您推荐本周必读资讯，分享他们的思考与观点\nFT商学院高端专享\nFT中文网2021年度论坛门票2张",
            smallPrint = "注：所有活动门票不可折算现金、不能转让、不含差旅与食宿",
            plans = listOf(
                Plan(
                    id = "plan_rLIy6LJYW8LV",
                    productId = "prod_zSgOTS6DWLmu",
                    price =  1998.0,
                    tier = Tier.PREMIUM,
                    cycle = Cycle.YEAR,
                    description = "Premium Yearly Plan",
                    discount = Discount(
                        id = "dsc_a1Vp92cfFAih",
                        priceOff =  300.0,
                        startUtc = ZonedDateTime.parse("2021-02-01T04:00:00Z"),
                        endUtc =  ZonedDateTime.parse("2021-02-07T16:00:00Z"),
                    )
                )
            )
        ),
    ),
)
