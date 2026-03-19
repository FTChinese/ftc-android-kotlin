package com.ft.ftchinese.ui.subs.catalog

import androidx.core.text.HtmlCompat
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.ftcsubs.Discount
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CheckoutIntent
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogOption
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogPlan
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogProduct
import com.ft.ftchinese.model.stripesubs.StripeCoupon
import com.ft.ftchinese.model.stripesubs.StripePrice

fun buildCatalogFtcCartItem(
    membership: Membership,
    product: SubscriptionCatalogProduct,
    plan: SubscriptionCatalogPlan,
    option: SubscriptionCatalogOption,
): CartItemFtc? {
    val tier = product.tier ?: return null
    val cycle = Cycle.fromString(plan.cycle.lowercase()) ?: return null
    val lookupKey = option.checkout.ftcPriceId.ifBlank { return null }
    val displayAmount = parseMoneyAmount(option.displayPrice) ?: return null
    val originalAmount = parseMoneyAmount(option.originalPrice)
    val currency = detectCurrency(option.displayPrice, option.originalPrice)

    val price = Price(
        id = lookupKey,
        tier = tier,
        cycle = cycle,
        active = true,
        currency = currency,
        kind = null,
        liveMode = true,
        nickname = sanitizeText(plan.title).ifBlank { sanitizeText(product.name) },
        periodCount = YearMonthDay.of(cycle),
        productId = product.id.ifBlank { "${tier.symbol}-${cycle.symbol}" },
        stripePriceId = "",
        title = listOf(sanitizeText(product.name), sanitizeText(plan.title))
            .filter { it.isNotBlank() }
            .joinToString(" "),
        unitAmount = originalAmount ?: displayAmount,
        startUtc = null,
        endUtc = null,
        offers = emptyList(),
    )

    val discount = if (originalAmount != null && originalAmount > displayAmount) {
        Discount(
            id = "${lookupKey}_display_discount",
            currency = currency,
            description = null,
            kind = null,
            startUtc = null,
            endUtc = null,
            overridePeriod = YearMonthDay.zero(),
            priceOff = originalAmount - displayAmount,
            percent = null,
            priceId = lookupKey,
            recurring = false,
            liveMode = true,
            status = null,
        )
    } else {
        null
    }

    return CartItemFtc(
        intent = CheckoutIntent.ofFtc(membership, price),
        price = price,
        discount = discount,
        isIntro = false,
    )
}

fun buildCatalogStripeCartItem(
    membership: Membership,
    product: SubscriptionCatalogProduct,
    plan: SubscriptionCatalogPlan,
    option: SubscriptionCatalogOption,
): CartItemStripe? {
    val tier = product.tier ?: return null
    val cycle = Cycle.fromString(plan.cycle.lowercase()) ?: return null
    val priceId = option.checkout.stripePriceId.ifBlank { return null }
    val displayAmount = parseMoneyAmount(option.displayPrice) ?: return null
    val originalAmount = parseMoneyAmount(option.originalPrice)
    val currency = detectCurrency(option.displayPrice, option.originalPrice)

    val recurring = StripePrice(
        id = priceId,
        active = !option.disabled,
        currency = currency,
        isIntroductory = false,
        liveMode = true,
        nickname = sanitizeText(plan.title).ifBlank { sanitizeText(product.name) },
        productId = product.id.ifBlank { "${tier.symbol}-${cycle.symbol}" },
        periodCount = YearMonthDay.of(cycle),
        tier = tier,
        unitAmount = (displayAmount * 100).toInt(),
    )

    val coupon = if (
        option.checkout.stripeCouponId.isNotBlank()
        && originalAmount != null
        && originalAmount > displayAmount
    ) {
        StripeCoupon(
            id = option.checkout.stripeCouponId,
            amountOff = ((originalAmount - displayAmount) * 100).toInt(),
            currency = currency,
            redeemBy = 0,
            priceId = recurring.id,
        )
    } else {
        null
    }

    return CartItemStripe(
        intent = CheckoutIntent.ofStripe(
            source = membership,
            target = recurring,
            hasCoupon = coupon != null
        ),
        recurring = recurring,
        trial = null,
        coupon = coupon,
    )
}

private fun sanitizeText(value: String?): String {
    val normalized = value?.trim().orEmpty()
    if (normalized.isBlank()) {
        return ""
    }

    return HtmlCompat
        .fromHtml(normalized, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\u00A0', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun parseMoneyAmount(value: String?): Double? {
    val normalized = sanitizeText(value)
    if (normalized.isBlank()) {
        return null
    }

    val match = Regex("""([0-9]+(?:\.[0-9]+)?)""")
        .find(normalized.replace(",", ""))
        ?.groupValues
        ?.getOrNull(1)
        ?: return null

    return match.toDoubleOrNull()
}

private fun detectCurrency(vararg candidates: String?): String {
    val joined = candidates.joinToString(" ")
    return when {
        joined.contains("£") -> "gbp"
        joined.contains("$") -> "usd"
        else -> "cny"
    }
}
