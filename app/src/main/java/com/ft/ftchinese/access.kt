package com.ft.ftchinese

import android.app.Activity
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.Permission
import com.ft.ftchinese.ui.pay.MemberActivity
import com.ft.ftchinese.ui.pay.PaywallActivity
import com.ft.ftchinese.ui.pay.UpgradeActivity
import com.ft.ftchinese.user.CredentialsActivity
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
fun Activity.grantPermission(account: Account?, contentPerm: Permission): Boolean {
    if (account == null) {
        toast(R.string.prompt_login_to_read)
        CredentialsActivity.startForResult(this)
        return false
    }

    // Grant access
    if ((account.permission and contentPerm.id) > 0) {
        return true
    }

    // If user if denied access, reasons could be:
    // Not a member
    // Was a member, expired.
    // Not a premium but content requires premium.

    // If user is not or was not a member.
    // Take into account the content's permission type.
    // If content is premium-only, tell user to buy premium.
    if (!account.isMember) {
        // If use is not a member yet while content requires
        // premium, show premium product on top.
        if (contentPerm == Permission.PREMIUM) {
            toast(R.string.prompt_premium_only)
            PaywallActivity.start(this, true)
        } else {
            toast(R.string.prompt_subscribe_to_read)
            PaywallActivity.start(this)
        }

        return false
    }

    // User is a member, but the membership might be expired.
    // If expired, show user paywall.
    // If the content requires premium membership, paywall should
    // display premium card on top.
    if (account.membership.isExpired) {
        // If membership is expired, show paywall again,
        // and put premium product on top.
        if (contentPerm == Permission.PREMIUM) {
            toast(R.string.prompt_premium_only)
            PaywallActivity.start(this, true)
        } else {
            // If the article does not require PREMIUM,
            // go to MemberActivity and ask user to resubscribe.
            toast(R.string.prompt_membership_expired)
            MemberActivity.start(this)
        }

        return false
    }

    // If code reaches here, user must be a valid member.
    // Access denied could only be caused by premium content.
    toast(R.string.prompt_premium_only)
    UpgradeActivity.start(this, true)

    return false
}

