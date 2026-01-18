package com.ft.ftchinese.model.reader

import android.os.Parcelable
import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.fetch.*
import com.ft.ftchinese.model.invoice.AddOn
import com.ft.ftchinese.model.invoice.Invoice
import com.ft.ftchinese.model.serializer.DateAsStringSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit

@Parcelize
@Serializable
data class Membership(
    val tier: Tier? = null,
    val cycle: Cycle? = null,
    // ISO8601 format. Example: 2019-08-05
    @Serializable(with = DateAsStringSerializer::class)
    val expireDate: LocalDate? = null,
    val payMethod: PayMethod? = null,
    val stripeSubsId: String? = null,
    // If autoRenew is true, ignore expireDate.
    val autoRenew: Boolean = false,
    val status: StripeSubStatus? = null,
    val appleSubsId: String? = null,
    val b2bLicenceId: String? = null,
    val standardAddOn: Long = 0,
    val premiumAddOn: Long = 0,
    val vip: Boolean = false,
) : Parcelable {

    fun toJsonString(): String {
        return marshaller.encodeToString(this)
    }

    fun tierQueryVal(): String {
        return when (tier) {
            Tier.STANDARD -> when (cycle) {
                Cycle.YEAR -> "standard"
                Cycle.MONTH -> "standardmonthly"
                else -> "unknown"
            }
            Tier.PREMIUM -> "premium"
            else -> "unknown"
        }
    }
    // Renew expiration date if auto-renewal subscription expired.
    private fun renew(): Membership {
        return Membership(
            tier = tier,
            cycle = cycle,
            expireDate = cycle?.let {
                expireDate?.plus(it.period)
            } ?: expireDate,
            payMethod = payMethod,
            stripeSubsId = stripeSubsId,
            autoRenew = autoRenew,
            status = status,
            appleSubsId = appleSubsId,
            b2bLicenceId = b2bLicenceId,
            standardAddOn = standardAddOn,
            premiumAddOn = premiumAddOn,
            vip = vip
        )
    }

    private fun claimAddOn(): Membership {
        return Membership(
            tier = when {
                hasPremiumAddOn -> Tier.PREMIUM
                hasStandardAddOn -> Tier.STANDARD
                else -> tier
            },
            cycle = cycle,
            expireDate = when {
                hasPremiumAddOn -> expireDate?.plusDays(standardAddOn)
                hasStandardAddOn -> expireDate?.plusDays(premiumAddOn)
                else -> expireDate
            },
            payMethod = PayMethod.ALIPAY, // WECHAT also works. It doesn't matter.
            stripeSubsId = null,
            autoRenew = false,
            status = null,
            appleSubsId = null,
            b2bLicenceId = null,
            standardAddOn = if (hasPremiumAddOn) standardAddOn else 0,
            premiumAddOn = 0,
            vip = vip
        )
    }

    // Build new membership based on invoices from a new purchase.
    fun withInvoice(inv: Invoice?): Membership {
        if (inv == null) {
            return this
        }

        // If current membership comes from stripe, apple, or is a premium.
        // Only change the add-on days.
        if (inv.orderKind == OrderKind.AddOn) {
            return plusAddOn(inv.toAddOn())
        }

        // For order kind create, renew, and upgrade.
        return Membership(
            tier = inv.tier,
            cycle = inv.cycle,
            expireDate = inv.endUtc?.toLocalDate(),
            payMethod = inv.payMethod,
            stripeSubsId = null,
            autoRenew = false,
            status = null,
            appleSubsId = null,
            b2bLicenceId = null,
            standardAddOn = standardAddOn,
            premiumAddOn = premiumAddOn,
            vip = vip,
        )
    }

    private fun plusAddOn(addOn: AddOn): Membership {
        return Membership(
            tier = tier,
            cycle = cycle,
            expireDate = expireDate,
            payMethod = payMethod,
            stripeSubsId = stripeSubsId,
            autoRenew = autoRenew,
            status = status,
            appleSubsId = appleSubsId,
            b2bLicenceId = b2bLicenceId,
            standardAddOn = standardAddOn + addOn.standardAddOn,
            premiumAddOn = premiumAddOn + addOn.premiumAddOn,
            vip = vip,
        )
    }

    // Returns the final state of membership. The purpose of this
    // is to make the expiration date continuous as much as possible
    // so that when calculation permission or show it on ui,
    // we could reduce the complexity of determining the final state
    // of expiration time.
    // After calling this method, you only need to check expireDate
    // field.
    fun normalize(): Membership {
        if (vip) {
            return this
        }

        if (autoRenew) {
            if (!expired) {
                return this
            }

            return renew()
        }

        if (hasAddOn) {
            if (!expired) {
                return this
            }

            return claimAddOn()
        }

        return this
    }

    val addOns: List<Pair<Tier, Long>>
        get() = mutableListOf<Pair<Tier, Long>>().apply {
            if (hasPremiumAddOn) {
                add(Pair(Tier.PREMIUM, premiumAddOn))
            }
            if (hasStandardAddOn) {
                add(Pair(Tier.STANDARD, standardAddOn))
            }
        }

    val autoRenewMoment: AutoRenewMoment?
        get() = if (autoRenew && expireDate != null && cycle != null) {
            AutoRenewMoment(
                cycle = cycle,
                month = if (cycle == Cycle.YEAR) {
                    expireDate.monthValue
                } else null,
                date = expireDate.dayOfMonth
            )
        } else null

    // In case legacy purchase has payment method null.
    val normalizedPayMethod: PayMethod?
        get() = payMethod
            ?: if (tier != null) {
                PayMethod.ALIPAY
            } else {
                null
            }

    val isStripe: Boolean
        get() = payMethod == PayMethod.STRIPE && stripeSubsId != null

    val isInvalidStripe: Boolean
        get() = isStripe && status?.isInvalid() == true

    val isTrialing: Boolean
        get() = status == StripeSubStatus.Trialing

    val canCancelStripe: Boolean
        get() = if (payMethod != PayMethod.STRIPE) {
            false
        } else {
            autoRenew
        }

    val isZero: Boolean
        get() = tier == null

    val isB2b: Boolean
        get() = payMethod == PayMethod.B2B

    val unlinkToEmailOnly: Boolean
        get() = arrayOf(
            PayMethod.STRIPE,
            PayMethod.APPLE,
            PayMethod.B2B
        ).contains(payMethod)
    /**
     * What kind of offer an existing membership could enjoy
     * for next round of purchase when using wechat/alipay.
     * OfferKind.Promotion always applies to anyone.
     */
    val offerKinds: List<OfferKind>
        get() = when {
            // For zero membership.
            tier == null -> listOf(
                OfferKind.Promotion,
                OfferKind.Introductory,
            )
            // Actually expired.
            autoRenewOffExpired -> listOf(
                OfferKind.Promotion,
                OfferKind.WinBack,
            )
            // Current valid.
            else -> listOf(
                OfferKind.Promotion,
                OfferKind.Retention
            )
        }

    /**
     * Determines whether the Renew button should be visible.
     * This is only applicable to alipay or wechat pay.
     * Status of a membership when its expire date falls into
     * various period.
     *         today              3 years later
     * --------- | -------------- | ---------
     * expired      renew/upgrade   upgrade only for standard
     *
     * Calling it manual renewal might be more proper.
     * This should only be available if current membership
     * is purchased via ali or wx.
     */
    fun beyondMaxRenewalPeriod(): Boolean {
        if (expireDate == null) {
            return false
        }

        val threeYearsLater = LocalDate.now().plusYears(3)

        return expireDate.isAfter(threeYearsLater)
    }

    // Tests if the expiration date is before today.
    // This does not take into account whether user is using auto renewal subscription.
    val expired: Boolean
        get() = when {
            vip -> false
            expireDate == null -> true
            else -> expireDate.isBefore(LocalDate.now())
        }

    // Precedence when determining whether it is actually expired:
    // 1. Auto Renew
    // 2. Expiration Date
    // 3. Has AddOns
    val autoRenewOffExpired: Boolean
        get() = !autoRenew && expired

    val hasAddOn: Boolean
        get() = standardAddOn > 0 || premiumAddOn > 0

    val hasStandardAddOn: Boolean
        get() = standardAddOn > 0

    val hasPremiumAddOn: Boolean
        get() = premiumAddOn > 0

    // Tests whether subscription time has moved to add-ons.
    // Usually you call this method without calling normalize method;
    // otherwise it won't reflect user's actual membership state.
    val shouldUseAddOn: Boolean
        get() = !autoRenew && expired && hasAddOn

    /**
     * Calculate remaining days before expiration.
     * This is only applicable to Alipay or Wechat pay.
     * You should check whether the subscription is stripe and in invalid state before checking this.
     * For invalid stripe, it is meaningless to calculate reaming days.
     */
    fun remainingDays(): Long? {
        if (expireDate == null) {
            return null
        }

        if (autoRenew) {
            return null
        }

        return LocalDate.now().until(expireDate, ChronoUnit.DAYS) + premiumAddOn + standardAddOn
    }



    // For auto renewal only show month or date.
    // Use getMonthValue() and getDayOfMonth() with a formatter string.
    fun localizeExpireDate(ctx: Context? = null): String {
        if (vip) {
            return ctx?.getString(R.string.member_long_term) ?: "长期有效"
        }

        if (expireDate == null) {
            return ""
        }

        return expireDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * getPermission calculates a membership's (including
     * zero value) permission to access content.
     * It uses binary operation to get the combined access
     * rights of a reader.
     * Each position in a binary number represents a unique
     * permission.
     * Returns an int representing the permission, and
     * membership status used to tell user what went wrong.
     */
    fun accessRights(): Pair<Int, MemberStatus> {

        if (vip) {
            return Pair(
                    Permission.FREE.id or Permission.STANDARD.id or Permission.PREMIUM.id,
                    MemberStatus.Vip
            )
        }

        // Not a member yet.
        if (tier == null) {
            return Pair(
                    Permission.FREE.id,
                    MemberStatus.Empty
            )
        }

        // Expired.
        if (expired) {

            return Pair(
                    Permission.FREE.id,
                    MemberStatus.Expired
            )
        }

        return when (tier) {
            Tier.STANDARD -> Pair(
                Permission.FREE.id or Permission.STANDARD.id,
                MemberStatus.ActiveStandard
            )
            Tier.PREMIUM -> Pair(
                Permission.FREE.id or Permission.STANDARD.id or Permission.PREMIUM.id,
                MemberStatus.ActivePremium
            )
        }
    }

    fun carryOverInvoice(): Invoice? {
        if (tier == null || cycle == null) {
            return null
        }

        return Invoice(
            id = "",
            compoundId = "",
            tier = tier,
            cycle = cycle,
            years = 0,
            months = 0,
            days = remainingDays()?.toInt() ?: 0,
            addOnSource = AddOnSource.CarryOver,
            appleTxId = null,
            orderId = null,
            orderKind = OrderKind.AddOn,
            paidAmount = 0.0,
            payMethod = payMethod,
            priceId = null,
            stripeSubsId = null,
            createdUtc = ZonedDateTime.now(),
            consumedUtc = null,
            startUtc = null,
            endUtc = null,
            carriedOverUtc = null,
        )
    }
}
