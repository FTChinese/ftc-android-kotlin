package com.ft.ftchinese.model.reader

import com.ft.ftchinese.model.enums.Tier

/**
 * Membership status corresponding to a Permission.
 * Permission matrix for various content
 *                  FREE    STANDARD    PREMIUM
 * NotLoggedIn      Y        N          N
 * Empty            Y        N          N
 * Expired          Y        N          N
 * InactiveStripe   Y        N          N
 * ActiveStandard   Y        Y          N
 * ActivePremium    Y        Y          Y
 * Vip              Y        Y          Y
 */
enum class MemberStatus {
    NotLoggedIn, // Thus cannot be determined.
    Empty, // Show Subscribe button only
    Expired, // Show Subscribe button only
    InactiveStripe, // if Member.status.shouldResubscribe, show Subscribe button only
    ActiveStandard, // Always show Upgrade button.
    ActivePremium,
    // For ActiveStandard and ActivePremium, if autoRenew, do not show Renew button; else test whether expireDate is within 3 year, and show Renew button if within allowed range.
    Vip; // MemberActivity do not show any buttons

    companion object {
        @JvmStatic
        fun of(account: Account?): MemberStatus {
            if (account == null) {
                return NotLoggedIn
            }

            val m = account.membership
            if (m.vip) {
                return Vip
            }

            if (m.tier == null) {
                return Empty
            }

            if (m.expired) {
                return Expired
            }

            return when (m.tier) {
                Tier.STANDARD -> ActiveStandard
                Tier.PREMIUM -> ActivePremium
            }
        }
    }
}
