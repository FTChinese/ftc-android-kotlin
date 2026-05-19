package com.ft.ftchinese.model.subscriptioncatalog

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.serializer.DateAsStringSerializer
import com.ft.ftchinese.model.serializer.LenientPayMethodSerializer
import com.ft.ftchinese.model.stripesubs.StripePendingChange
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
                    val couponAmountOff = it.applicableCoupon()?.amountOff ?: 0
                    SubscriptionCheckout(
                        stripePriceId = it.price.id,
                        stripeTrialPriceId = "",
                        stripeCouponId = it.applicableCoupon()?.id.orEmpty(),
                        stripeCurrency = it.price.currency,
                        stripeUnitAmount = it.price.unitAmount,
                        stripePayableAmount = (it.price.unitAmount - couponAmountOff).coerceAtLeast(0),
                        stripeCouponAmountOff = couponAmountOff,
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
                                                    },
                                                    stripeCurrency = if (option.checkout.stripeCurrency.isBlank()) {
                                                        stripeCheckout.stripeCurrency
                                                    } else {
                                                        option.checkout.stripeCurrency
                                                    },
                                                    stripeUnitAmount = if (option.checkout.stripeUnitAmount <= 0) {
                                                        stripeCheckout.stripeUnitAmount
                                                    } else {
                                                        option.checkout.stripeUnitAmount
                                                    },
                                                    stripePayableAmount = if (option.checkout.stripePayableAmount <= 0) {
                                                        stripeCheckout.stripePayableAmount
                                                    } else {
                                                        option.checkout.stripePayableAmount
                                                    },
                                                    stripeCouponAmountOff = if (option.checkout.stripeCouponAmountOff <= 0) {
                                                        stripeCheckout.stripeCouponAmountOff
                                                    } else {
                                                        option.checkout.stripeCouponAmountOff
                                                    },
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
    val stripePriceId: String = "",
    val stripeCurrency: String = "",
    val pendingStripeChange: StripePendingChange? = null,
) {
    fun checkoutMembership(fallback: Membership): Membership {
        val normalizedStatus = status.lowercase()
        val cycle = Cycle.fromString(billingCycle?.lowercase()) ?: fallback.cycle

        if (normalizedStatus == "none") {
            return Membership()
        }

        if (normalizedStatus == "expired") {
            return fallback.copy(
                tier = tier,
                cycle = cycle,
                expireDate = expireDate,
                payMethod = null,
                stripeSubsId = null,
                autoRenew = false,
                status = null,
                appleSubsId = null,
                b2bLicenceId = null,
                pendingStripeChange = null,
            )
        }

        if (normalizedStatus != "active") {
            return fallback
        }

        return fallback.copy(
            tier = tier,
            cycle = cycle,
            expireDate = expireDate,
            payMethod = payMethod,
            stripeSubsId = if (payMethod == PayMethod.STRIPE) {
                fallback.stripeSubsId
            } else {
                null
            },
            autoRenew = autoRenew,
            status = if (payMethod == PayMethod.STRIPE) {
                fallback.status
            } else {
                null
            },
            pendingStripeChange = if (payMethod == PayMethod.STRIPE) {
                pendingStripeChange ?: fallback.pendingStripeChange
            } else {
                null
            },
            appleSubsId = if (payMethod == PayMethod.APPLE) {
                fallback.appleSubsId
            } else {
                null
            },
            b2bLicenceId = if (payMethod == PayMethod.B2B) {
                fallback.b2bLicenceId
            } else {
                null
            },
        )
    }
}

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
    val stripeCurrency: String = "",
    val stripeUnitAmount: Int = 0,
    val stripePayableAmount: Int = 0,
    val stripeCouponAmountOff: Int = 0,
    val ftcPriceId: String = "",
)
