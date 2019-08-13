package com.ft.ftchinese.ui.pay

import android.app.Activity
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.Permission
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.model.order.StripeSubStatus
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.util.RequestCode
import org.jetbrains.anko.toast

/**
 * Access control of member to content.
 * It first checks whether account membership is valid.
 * If valid, then checks access against content's required
 * access.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
fun Activity.grantPermission(account: Account?, contentPerm: Permission): Boolean {

    // Grant access immediately if content is free.
    if (contentPerm == Permission.FREE) {
        return true
    }

    // Content requires membership.
    if (account == null) {
        toast(R.string.prompt_login_to_read)
        LoginActivity.startForResult(this)
        return false
    }

    if (account.isVip) {
        return true
    }

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

    // If stripe subscription status is not active.
    if (account.membership.payMethod == PayMethod.STRIPE) {
        if (account.membership.status != StripeSubStatus.Active) {
            toast(R.string.stripe_not_active)
            MemberActivity.start(this)
            return false
        }
    }

    // User is a member, but the membership might be expired.
    // If expired, show user paywall.
    // If the content requires premium membership, paywall should
    // display premium card on top.
    if (account.membership.expired() && account.membership.autoRenew == false) {
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


    // Grant access
    if ((account.permission and contentPerm.id) <= 0) {
        toast(R.string.prompt_premium_only)
        UpgradeActivity.startForResult(this, RequestCode.PAYMENT, true)
        return false
    }

    // If code reaches here, user must be a valid member.
    // Access denied could only be caused by premium content.
    return true
}
