package com.ft.ftchinese.model

import android.os.Parcelable
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KDate
import com.ft.ftchinese.util.KPayMethod
import com.ft.ftchinese.util.KTier
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.LocalDate

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
        val autoRenew: Boolean? = false
) : Parcelable {
    /**
     * Check if membership is expired.
     * @return true if expireDate is before now, or membership does not exist.
     */
    val isExpired: Boolean
        get() = expireDate
                    ?.isBefore(LocalDate.now())
                    ?: true

    fun fromWxOrAli(): Boolean {
        // The first condition is used for backward compatibility.
        return (tier != null && payMethod == null) || payMethod == PayMethod.ALIPAY || payMethod == PayMethod.WXPAY
    }

    // Determine how user is using CheckOutActivity.
    fun subType(plan: Plan?): OrderUsage? {
        if (plan == null) {
            return null
        }

        if (tier == null) {
            return OrderUsage.CREATE
        }

        if (isExpired) {
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

    fun canUseStripe(): Boolean {
        if (isExpired) {
            if (fromWxOrAli()) {
                return true
            }
            if (autoRenew == null || autoRenew) {
                return false
            }

            return true
        }

        // If current member is not expired, user is not
        // allowed to use tripe regardless of payment method.
        return false
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
     * This is only applicable to alipay or wechat pay.
     * Status of a membership when its expire date falls into
     * various period.
     *         today              3 years later
     * --------- | -------------- | ---------
     * expired      renew/upgrade   upgrade only for standard
     */
    fun canRenew(): Boolean {
        if (expireDate == null) {
            return false
        }

        val today = LocalDate.now()
        val threeYearsLater = today.plusYears(3)

        return expireDate.isBefore(threeYearsLater)
    }

    /**
     * Only when user's current tier is standard should
     * upgrading be allowed.
     * Actually you also take into account expire date.
     * Only not-yet-expired member should allow upgrading.
     * If membership is expired, simply ask user to subscribe
     * again.
     */
//    fun allowUpgrade(): Boolean {
//        return tier == Tier.STANDARD
//    }

    // Compare expireDate against another instance.
    // Pick whichever is later.
    fun isNewer(m: Membership): Boolean {
        if (expireDate == null && m.expireDate == null) {
            return false
        }

        if (m.expireDate == null) {
            return true
        }

        if (expireDate == null) {
            return false
        }

        return expireDate.isAfter(m.expireDate)
    }
}

