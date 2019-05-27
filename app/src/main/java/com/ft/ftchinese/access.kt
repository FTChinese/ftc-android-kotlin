package com.ft.ftchinese

import android.app.Activity
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.Tier
import com.ft.ftchinese.ui.pay.MemberActivity
import com.ft.ftchinese.ui.pay.PaywallActivity
import com.ft.ftchinese.ui.pay.UpgradeActivity
import com.ft.ftchinese.user.CredentialsActivity
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
fun Activity.shouldGrantStandard(account: Account?): Boolean {
    if (account == null) {
      toast(R.string.prompt_login_to_read)
        CredentialsActivity.startForResult(this)
      return false
    }

    if (account.isVip) {
        return true
    }

    if (!account.isMember) {
        toast(R.string.prompt_subscribe_to_read)
        PaywallActivity.start(this)
        return false
    }

    if (account.membership.isExpired) {
        toast(R.string.prompt_membership_expired)
        MemberActivity.start(this)
        return false
    }

    return true
}

@kotlinx.coroutines.ExperimentalCoroutinesApi
fun Activity.shouldGrantPremium(account: Account?): Boolean {
    if (account == null) {
        toast(R.string.prompt_login_to_read)
        CredentialsActivity.startForResult(this)
        return false
    }

    if (account.isVip) {
        return true
    }

    if (!account.isMember) {
        toast(R.string.prompt_subscribe_to_read)
        PaywallActivity.start(this)
        return false
    }

    if (account.membership.isExpired) {
        toast(R.string.prompt_membership_expired)
        MemberActivity.start(this)
        return false
    }

    if (account.membership.tier != Tier.PREMIUM) {
        toast(R.string.prompt_premium_only)
        UpgradeActivity.start(this, true)
        return false
    }

    return true
}