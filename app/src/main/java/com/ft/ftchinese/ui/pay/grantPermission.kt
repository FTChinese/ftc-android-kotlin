package com.ft.ftchinese.ui.pay

import android.app.Activity
import com.ft.ftchinese.R
import com.ft.ftchinese.model.MemberStatus
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.Permission
import com.ft.ftchinese.ui.login.LoginActivity
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
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

    // Content is not free.
    val permResult = account?.membership?.getPermission()


    // Indicates reader is not logged in.
    if (permResult == null) {
        toast(R.string.prompt_login_to_read)
        LoginActivity.startForResult(this)
        return false
    }


    val (readerPermBits, status) = permResult

    // Free content is excluded when reaching here.
    if ((readerPermBits and contentPerm.id) > 0) {
        return true
    }

    if (status == MemberStatus.InactiveStripe) {
        toast(R.string.stripe_not_active)
        MemberActivity.start(this)
        return false
    }

    if (status == MemberStatus.Empty) {
        toast(R.string.prompt_member_only)
        PaywallActivity.start(this, contentPerm == Permission.PREMIUM)
        return false
    }

    if (status == MemberStatus.Expired) {
        toast(R.string.prompt_premium_only)
        PaywallActivity.start(this, contentPerm == Permission.PREMIUM)

        return false
    }

    if (status == MemberStatus.ActiveStandard) {
        toast(R.string.prompt_upgrade_premium)
        MemberActivity.start(this)

        return false
    }


    alert(Appcompat, "There might be problems with your permission. Please check your membership or contact our customer service", "Error") {
        positiveButton("OK") {
            it.dismiss()
        }
    }

    return false
}
