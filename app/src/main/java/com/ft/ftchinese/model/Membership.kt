package com.ft.ftchinese.model

import android.os.Parcelable
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.util.*
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit

@Parcelize
data class Membership(
        val id: String? = null,
        @KTier
        val tier: Tier? = null,
        @KCycle
        val cycle: Cycle? = null,
        // ISO8601 format. Example: 2019-08-05
        @KDate
        val expireDate: LocalDate? = null,
        @KPayMethod
        val payMethod: PayMethod? = null,
        val autoRenew: Boolean? = false,
        @KStripeSubStatus
        val status: StripeSubStatus? = null,
        val vip: Boolean = false
) : Parcelable {

    fun withSubscription(s: Subscription): Membership {
        return Membership(
                id = id,
                tier = s.tier,
                cycle = s.cycle,
                expireDate = s.endDate,
                payMethod = s.payMethod,
                autoRenew = autoRenew,
                status = status
        )
    }

    fun remainingDays(): Long? {
        if (expireDate == null) {
            return null
        }

        if (expired()) {
            return null
        }

        return LocalDate.now().until(expireDate, ChronoUnit.DAYS)
    }

    /**
     * Checks whether membership expired.
     * For stripe customer, if expire date is past and
     * authRenew is true, take it as not expired.
     */
    fun expired(): Boolean {
        if (expireDate == null) {
            return true
        }
        return expireDate.isBefore(LocalDate.now()) && (autoRenew == false)
    }

    fun fromWxOrAli(): Boolean {
        // The first condition is used for backward compatibility.
        return (tier != null && payMethod == null) || payMethod == PayMethod.ALIPAY || payMethod == PayMethod.WXPAY
    }

    // Determine how user is using CheckOutActivity.
    fun subType(plan: Plan?): OrderUsage? {
        if (plan == null) {
            return null
        }

        if (tier == null || shouldResubscribe()) {
            return OrderUsage.CREATE
        }

        if (tier == plan.tier) {
            return OrderUsage.RENEW
        }

        if (tier == Tier.STANDARD && plan.tier == Tier.PREMIUM) {
            return OrderUsage.UPGRADE
        }

        return null
    }

    fun getPlan(): Plan? {
        if (tier == null) {
            return null
        }

        if (cycle == null) {
            return null
        }

        return subsPlans.of(tier, cycle)
    }

    /**
     * Determines whether the Re-Subscribe button should
     * be visible.
     */
    fun shouldResubscribe(): Boolean {
        if (expired()) {
            return true
        }

        if (status?.shouldResubscribe() == true) {
            return true
        }

        return false
    }

    /**
     * Determines whether the Renew button should be visible.
     * This is only applicable to alipay or wechat pay.
     * Status of a membership when its expire date falls into
     * various period.
     *         today              3 years later
     * --------- | -------------- | ---------
     * expired      renew/upgrade   upgrade only for standard
     */
    fun shouldRenew(): Boolean {
        if (expireDate == null) {
            return false
        }

        if (payMethod == PayMethod.STRIPE) {
            return false
        }

        val today = LocalDate.now()
        val threeYearsLater = today.plusYears(3)

        return expireDate.isBefore(threeYearsLater)
    }

    /**
     * Determines whether the Upgrade button should be
     * visible.
     */
    fun shouldUpgrade(): Boolean {
        if (tier == null) {
            return false
        }

        return tier == Tier.STANDARD
    }

    /**
     * Determine whether to display a warning message
     * on membership info.
     */
    fun isActiveStripe(): Boolean {
        return payMethod == PayMethod.STRIPE && status == StripeSubStatus.Active
    }

    /**
     * Determines whether the stripe payment method should
     * be visible.
     */
    fun permitStripe(): Boolean {
        if (tier == null) {
            return true
        }

        return shouldResubscribe()
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

        // For upgrading we canno compare the expire date.
        if (tier == Tier.STANDARD && remote.tier
         == Tier.PREMIUM) {
            return true
        }

        return false
    }
}

