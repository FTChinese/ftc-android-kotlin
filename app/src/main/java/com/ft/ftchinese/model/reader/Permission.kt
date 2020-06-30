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

// Test whether we could grant an account access to a content.
// Free content is alway accessible by anyone.
// Standard and Premium content always require user login.
fun denyPermission(account: Account?, contentPerm: Permission): MemberStatus? {
    if (contentPerm == Permission.FREE) {
        return null
    }

    if (account == null) {
        return MemberStatus.NotLoggedIn
    }

    val (readerPermBits, status) = account.membership.getPermission()

    // If reader's membership permission bits includes
    // content permission, grant access.
    // For Vip and ActivePremium it is always true;
    // For ActiveStandard only true when contentPerm is STANDARD.
    if ((readerPermBits and contentPerm.id) > 0) {
        return null
    }

    // Otherwise return user's current membership status
    // so that we could further determine the cause of denial.
    // The denied status incluedes:
    // Empty;
    // Expired;
    // InactiveStripe;
    // ActiveStandard, this indicates the content is Premium.
    return status
}

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
    Vip // MemberActivity do not show any buttons
}

/**
 * Deduce what kind of actions current membership could take.
 */
enum class NextStep(val id: Int) {
    None(0),
    Resubscribe(1),
    Renew(2),
    Upgrade(4)
}

