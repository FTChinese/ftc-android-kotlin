package com.ft.ftchinese.models

import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KDate
import com.ft.ftchinese.util.KTier
import org.threeten.bp.LocalDate

data class Membership(
        @KTier
        val tier: Tier? = null,
        @KCycle
        val cycle: Cycle? = null,
        // ISO8601 format. Example: 2019-08-05
        @KDate
        val expireDate: LocalDate? = null
) {
    /**
     * Check if membership is expired.
     * @return true if expireDate is before now, or membership does not exist.
     */
    val isExpired: Boolean
        get() = expireDate
                    ?.isBefore(LocalDate.now())
                    ?: true

    /**
     * Status of a membership when its expire date falls into
     * various period.
     *         today              3 years later
     * --------- | -------------- | ---------
     * expired      renew/upgrade   upgrade only for standard
     */
    fun getStatus(): MemberStatus {
        if (expireDate == null) {
            return MemberStatus.INVALID
        }

        val today = LocalDate.now()
        val threeYearsLater = today.plusYears(3)

        return when {
            expireDate.isBefore(today) -> MemberStatus.EXPIRED
            expireDate.isBefore(threeYearsLater) -> MemberStatus.RENEWABLE
            else -> MemberStatus.BEYOND_RENEW
        }
    }

    /**
     * Only when user's current tier is standard should
     * upgrading be allowed.
     * Actually you also take into account expire date.
     * Only not-yet-expired member should allow upgrading.
     * If membership is expired, simply ask user to subscribe
     * again.
     */
    fun allowUpgrade(): Boolean {
        return tier == Tier.STANDARD
    }

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

