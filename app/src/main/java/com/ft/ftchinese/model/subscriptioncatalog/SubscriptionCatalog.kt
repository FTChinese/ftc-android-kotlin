package com.ft.ftchinese.model.subscriptioncatalog

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.serializer.DateAsStringSerializer
import com.ft.ftchinese.model.serializer.LenientPayMethodSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDate

@Serializable
data class SubscriptionCatalog(
    val preferredLanguage: String = "zh",
    val summary: SubscriptionCatalogSummary = SubscriptionCatalogSummary(),
    val hero: SubscriptionCatalogHero = SubscriptionCatalogHero(),
    val actionsTitle: String = "",
    val products: List<SubscriptionCatalogProduct> = emptyList(),
) {
    val hasProducts: Boolean
        get() = products.isNotEmpty()

    fun reOrderProducts(premiumOnTop: Boolean): SubscriptionCatalog {
        return if (!premiumOnTop) {
            this
        } else {
            copy(
                products = products.sortedByDescending { it.tier?.ordinal ?: -1 }
            )
        }
    }

    fun hydrateCheckoutData(paywall: Paywall?): SubscriptionCatalog {
        if (paywall == null) {
            return this
        }

        val ftcIndex = paywall.products
            .flatMap { product ->
                product.prices.mapNotNull { price ->
                    val cycle = price.cycle?.name?.lowercase() ?: return@mapNotNull null
                    product.tier to (cycle to price.id)
                }
            }
            .groupBy({ it.first to it.second.first }, { it.second.second })
            .mapValues { (_, ids) -> ids.firstOrNull().orEmpty() }

        val stripeIndex = paywall.stripe
            .filter { it.price.active && !it.price.isIntroductory }
            .associateBy(
                keySelector = {
                    it.price.tier to it.price.periodCount.toCycle().name.lowercase()
                },
                valueTransform = {
                    SubscriptionCheckout(
                        stripePriceId = it.price.id,
                        stripeTrialPriceId = "",
                        stripeCouponId = it.applicableCoupon()?.id.orEmpty(),
                        ftcPriceId = ""
                    )
                }
            )

        return copy(
            products = products.map { product ->
                val tier = product.tier
                product.copy(
                    plans = product.plans.map { plan ->
                        val cycle = plan.cycle.lowercase()
                        val ftcPriceId = tier?.let { ftcIndex[it to cycle] }.orEmpty()
                        val stripeCheckout = tier?.let { stripeIndex[it to cycle] }

                        plan.copy(
                            options = plan.options.map { option ->
                                when (option.kind) {
                                    "ftc" -> {
                                        if (option.checkout.ftcPriceId.isNotBlank() || ftcPriceId.isBlank()) {
                                            option
                                        } else {
                                            option.copy(
                                                checkout = option.checkout.copy(ftcPriceId = ftcPriceId)
                                            )
                                        }
                                    }

                                    "stripe" -> {
                                        val currentStripeId = option.checkout.stripePriceId
                                        if ((currentStripeId.startsWith("price_") && currentStripeId.isNotBlank()) || stripeCheckout == null) {
                                            option
                                        } else {
                                            option.copy(
                                                checkout = option.checkout.copy(
                                                    stripePriceId = stripeCheckout.stripePriceId,
                                                    stripeCouponId = if (option.checkout.stripeCouponId.isBlank()) {
                                                        stripeCheckout.stripeCouponId
                                                    } else {
                                                        option.checkout.stripeCouponId
                                                    }
                                                )
                                            )
                                        }
                                    }

                                    else -> option
                                }
                            }
                        )
                    }
                )
            }
        )
    }
}

@Serializable
data class SubscriptionCatalogSummary(
    val status: String = "none",
    val statusText: String = "",
    val tier: Tier? = null,
    @Serializable(with = LenientPayMethodSerializer::class)
    val payMethod: PayMethod? = null,
    @Serializable(with = DateAsStringSerializer::class)
    val expireDate: LocalDate? = null,
    val billingCycle: String? = null,
    val autoRenew: Boolean = false,
)

@Serializable
data class SubscriptionCatalogHero(
    val show: Boolean = false,
    val title: String = "",
    val detail: String = "",
)

@Serializable
data class SubscriptionCatalogProduct(
    val id: String = "",
    val tier: Tier? = null,
    val name: String = "",
    val note: String = "",
    val benefits: List<String> = emptyList(),
    val statusText: String = "",
    val plans: List<SubscriptionCatalogPlan> = emptyList(),
)

@Serializable
data class SubscriptionCatalogPlan(
    val id: String = "",
    val cycle: String = "",
    val title: String = "",
    val statusText: String = "",
    val options: List<SubscriptionCatalogOption> = emptyList(),
)

@Serializable
data class SubscriptionCatalogOption(
    val id: String = "",
    val kind: String = "",
    val renewalMode: String = "",
    val title: String = "",
    val paymentLabel: String = "",
    val displayPrice: String = "",
    val originalPrice: String = "",
    val isActive: Boolean = false,
    val disabled: Boolean = false,
    val ctaText: String = "",
    val checkout: SubscriptionCheckout = SubscriptionCheckout(),
)

@Serializable
data class SubscriptionCheckout(
    val stripePriceId: String = "",
    val stripeTrialPriceId: String = "",
    val stripeCouponId: String = "",
    val ftcPriceId: String = "",
)
