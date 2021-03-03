package com.ft.ftchinese.model.reader

import android.os.Parcelable
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.*
import com.ft.ftchinese.model.order.StripeSubStatus
import kotlinx.parcelize.Parcelize
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit

@Parcelize
data class Membership(
    @KTier
    val tier: Tier? = null,
    @KCycle
    val cycle: Cycle? = null,

    // ISO8601 format. Example: 2019-08-05
    @KDate
    val expireDate: LocalDate? = null,
    @KPayMethod
    val payMethod: PayMethod? = null,
    val stripeSubsId: String? = null,
    // If autoRenew is true, ignore expireDate.
    val autoRenew: Boolean = false,
    @KStripeSubStatus
    val status: StripeSubStatus? = null,
    val appleSubsId: String? = null,
    val b2bLicenceId: String? = null,
    val standardAddOn: Long = 0,
    val premiumAddOn: Long = 0,
    val vip: Boolean = false,
) : Parcelable {

    // Renew expiration date if auto-renewal subscription expired.
    private fun renew(): Membership {
        return Membership(
            tier = tier,
            cycle = cycle,
            expireDate = when (cycle) {
                Cycle.YEAR -> expireDate?.plusYears(1L)
                Cycle.MONTH -> expireDate?.plusMonths(1L)
                else -> expireDate
            },
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

    private fun useAddOn(): Membership {
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

            return useAddOn()
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

    val tierStringRes: Int
        get() = when {
            vip -> R.string.tier_vip
            tier != null -> tier
                    .stringRes
            else -> R.string.tier_free
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

    // Checks whether the current membership is purchased via alipay or wechat pay.
    val isOneTimePurchase: Boolean
        get() = if (tier != null && payMethod == null) {
            true
        } else {
            payMethod == PayMethod.ALIPAY || payMethod == PayMethod.WXPAY
        }

    val isStripe: Boolean
        get() = payMethod == PayMethod.STRIPE && stripeSubsId != null

    val isInvalidStripe: Boolean
        get() = isStripe && status?.isInvalid() == true

    val canCancelStripe: Boolean
        get() = if (payMethod != PayMethod.STRIPE) {
            false
        } else {
            autoRenew
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
    fun withinAliWxRenewalPeriod(): Boolean {
        if (expireDate == null) {
            return true
        }

        val today = LocalDate.now()
        val threeYearsLater = today.plusYears(3)

        return expireDate.isBefore(threeYearsLater) && expireDate.isAfter(today)
    }

    // Tests if the expiration date is before today.
    // This does not take into account whether user is using auto renewal subscription.
    val expired: Boolean
        get() = when {
            vip -> false
            expireDate == null -> true
            isInvalidStripe -> true
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
    fun localizeExpireDate(): String {
        if (vip) {
            return "无限期"
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
    fun getPermission(): Pair<Int, MemberStatus?> {

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

        // Valid standard.
        if (tier == Tier.STANDARD) {
            return Pair(
                    Permission.FREE.id or Permission.STANDARD.id,
                    MemberStatus.ActiveStandard
            )
        }

        // Valid premium.
        if (tier == Tier.PREMIUM) {
            return Pair(
                    Permission.FREE.id or Permission.STANDARD.id or Permission.PREMIUM.id,
                    MemberStatus.ActivePremium
            )
        }

        return Pair(Permission.FREE.id, null)
    }

}

