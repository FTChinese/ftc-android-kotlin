package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.util.KDateTime
import org.threeten.bp.ZonedDateTime

data class Banner(
    val id: Int,
    val heading: String,
    val subHeading: String?,
    val coverUrl: String?,
    val content: String?,
    val promoId: String?
)

data class Promo(
    val id: String?,
    val heading: String?,
    val subHeading: String?,
    val coverUrl: String?,
    val content: String?,
    @KDateTime
    val startUtc: ZonedDateTime?,
    @KDateTime
    val endUtc: ZonedDateTime?
)

data class Paywall(
    val banner: Banner,
    val promo: Promo,
    val products: List<Product>
)

const val paywallCacheName = "paywall.json"
