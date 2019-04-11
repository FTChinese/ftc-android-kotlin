package com.ft.ftchinese.user

import android.app.Activity
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.Tier
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
        SubscriptionActivity.start(this)
        return false
    }

    if (account.membership.isExpired) {
        toast(R.string.prompt_membership_expired)
        MySubsActivity.start(this)
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
        SubscriptionActivity.start(this)
        return false
    }

    if (account.membership.isExpired) {
        toast(R.string.prompt_membership_expired)
        MySubsActivity.start(this)
        return false
    }

    if (account.membership.tier != Tier.PREMIUM) {
        toast(R.string.prompt_premium_only)
        return false
    }

    return true
}