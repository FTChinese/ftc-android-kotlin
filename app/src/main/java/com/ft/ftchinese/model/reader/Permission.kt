package com.ft.ftchinese.model.reader

/**
 * Permission uses bitwise operation represent an article's
 * access right.
 * An article should always have a single value of the enum,
 * while user's access should contain the combination of them.
 */
enum class Permission(val id: Int) {
    FREE(1), // 0001
    STANDARD(2), // 0010
    PREMIUM(4); // 0100

    fun grant(readerPermBits: Int): Boolean {
        return (readerPermBits and id) > 0
    }
}

enum class MemberStatus {
    Vip, // MemberActivity do not show any buttons
    Empty, // Show Subscribe button only
    Expired, // Show Subscribe button only
    InactiveStripe, // if Member.status.shouldResubscribe, show Subscribe button only
    ActiveStandard, // Always show Upgrade button.
    ActivePremium
    // For ActiveStandard and ActivePremium, if autoRenew, do not show Renew button; else test whether expireDate is within 3 year, and show Renew button if within allowed range.
}

/**
 * Determines which buttons are visible on membership info activity.
 */
enum class NextStep(val id: Int) {
    None(0),
    Resubscribe(1),
    Renew(2),
    Upgrade(4)
}

/**
 * Which buttons should be visible on MemberActivity
 */
data class VisibleButtons(
        val showSubscribe: Boolean,
        val showRenew: Boolean,
        val showUpgrade: Boolean
)
