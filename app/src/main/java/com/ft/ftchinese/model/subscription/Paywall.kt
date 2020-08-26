package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.util.KDateTime
import com.ft.ftchinese.util.KTier
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
    id = "promo_GsSUkKzJSqAd",
    heading = "FT中文网15周年庆",
    subHeading = "会员5折 全年最低",
    coverUrl = "http://www.ftacademy.cn/subscription.jpg",
    content = "15年来，FT中文网坚守独立客观、真实准确的报道原则，为中国菁英和决策者们提供不可或缺的商业财经新闻、深度分析及评论，帮助中国读者了解世界",
    terms = "活动截止时间：2020年9月2日（北京时间）\n月度标准会员、实体订阅卡、企业机构订户和Stripe支付订户不参加本次促销活动\n本次活动的最终解释权归FT中文网所有",
    startUtc = ZonedDateTime.parse("2020-08-18T16:00:00Z"),
    endUtc = ZonedDateTime.parse("2020-09-02T16:00:00Z")
)

const val paywallFileName = "paywall.json"
