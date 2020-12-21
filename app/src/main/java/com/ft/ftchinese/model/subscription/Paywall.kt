package com.ft.ftchinese.model.subscription

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
 * A hard-coded promotion.
 */
val promotion = Promo(
    id = "promo_wa3UWuiYKEgm",
    heading = "FT中文网会员订阅",
    subHeading = "双11限时85折，还送财经词汇！",
    coverUrl = "http://www.ftacademy.cn/subscription.jpg",
    content = "前1000名标准会员送价值69元的《英国\u003c金融时报\u003e财经词汇》简装版，高端会员送价值199元的精装版。\n未来365天，在原汁原味英文+资深团队译文中，取得语言能力的进步。在国际视野与多元言论里，让你站在高海拔的地方俯瞰世界，深入思考！",
    terms = "活动截止：2020年11月11日24点（北京时间）\n现有会员（含升级/续订）、实体订阅卡、企业机构会员和Stripe支付会员均不参加本次活动\n赠品配送地址仅限中国大陆，海外及港澳台地区订户可请大陆好友代收\n本次活动的最终解释权归FT中文网所有",
    startUtc = ZonedDateTime.parse("2020-11-09T04:00:00Z"),
    endUtc = ZonedDateTime.parse("2020-11-11T16:00:00Z")
)

