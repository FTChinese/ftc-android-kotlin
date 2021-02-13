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
    val standardAddOn: Int = 0,
    val premiumAddOn: Int = 0,
    val vip: Boolean = false,
) : Parcelable {

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

    /**
     * Checks whether the current membership is purchased
     * via alipay or wechat pay.
     */
    private fun isAliOrWxPay(): Boolean {
        // For backward compatibility with legacy db format.
        if (tier != null && payMethod == null) {
            return true
        }

        // The first condition is used for backward compatibility.
        return payMethod == PayMethod.ALIPAY || payMethod == PayMethod.WXPAY
    }

    fun isStripe(): Boolean {
        return payMethod == PayMethod.STRIPE && stripeSubsId != null
    }

    fun isInvalidStripe(): Boolean {
        return isStripe() && status?.isInvalid() == true
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
    /**
     * Determine if we should show upgrade button.
     * Only when membership is not expired yet, and created via
     * Alipay, Wechat, or Stripe, and standard edition should we allow
     * upgrade.
     */
    fun canUpgrade(): Boolean {
        if (!arrayOf(PayMethod.ALIPAY, PayMethod.STRIPE, PayMethod.WXPAY).contains(payMethod)) {
            return false
        }

        if (expired()) {
            return false
        }

        if (tier != Tier.STANDARD) {
            return false
        }

        return true
    }

    /**
     * Checks whether membership expired.
     * For stripe and apple iap, if expire date is past and
     * autoRenew is true, take it as not expired.
     * For stripe invalid status, take it as expired.
     */
    fun expired(): Boolean {
        if (vip) {
            return false
        }

        if (expireDate == null) {
            return true
        }

        if (isInvalidStripe()) {
            return true
        }

        return expireDate.isBefore(LocalDate.now()) && !autoRenew
    }

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

        if (!isAliOrWxPay()) {
            return null
        }

        return LocalDate.now().until(expireDate, ChronoUnit.DAYS)
    }

    fun canCancelStripe(): Boolean {
        if (payMethod != PayMethod.STRIPE) {
            return false
        }

        // As long as auto renew is on, we should allow cancel.
        return autoRenew
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
        if (expired()) {
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

    /**
     * Determines whether the stripe payment method should
     * be visible.
     */
    fun permitStripe(): Boolean {
        if (tier == null) {
            return true
        }

        // expired and not auto renew.
        if (expired()) {
            return true
        }

        if (status?.isInvalid() == true) {
            return true
        }

        return false
    }

    /**
     * Compare local membership against remote.
     */
    fun useRemote(remote: Membership): Boolean {
        if (expireDate == null && remote.expireDate == null) {
            return false
        }

        if (remote.expireDate == null) {
            return false
        }

        if (expireDate == null) {
            return true
        }

        // Renewal
        if (tier == remote.tier) {
            return remote.expireDate.isAfter(expireDate)
        }

        // For upgrading we cannot compare the expire date.
        if (tier == Tier.STANDARD && remote.tier
         == Tier.PREMIUM) {
            return true
        }

        return false
    }
}

