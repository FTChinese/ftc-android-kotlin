package com.ft.ftchinese.ui.pay

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.NextStep
import com.ft.ftchinese.model.subscription.Tier

data class UIMemberInfo(
        val tier: String,
        val expireDate: String,
        val autoRenewal: Boolean,
        val stripeStatus: String?, // Show stripe status or hide it if null
        val stripeInactive: Boolean, // Show stripe status warning
        val remains: String?, // Show remaining days that will expire or expired.
        val isValidIAP: Boolean
)

/**
 * Which buttons should present based on current membership
 * status.
 * For vip, show nothing;
 * For membership that is empty, expired, inactive stripe, show subscription button;
 * For active standard, show renewal button only if within renewal range, and always show upgrade button;
 * For active premium show renewal buttons if within renewal range, otherwise none;
 */
data class UIMemberNextSteps(
        val reSubscribe: Boolean,
        val renew: Boolean,
        val upgrade: Boolean
)

fun buildNextStepButtons(steps: Int): UIMemberNextSteps {
    return UIMemberNextSteps(
            reSubscribe = (steps and NextStep.Resubscribe.id) > 0,
            renew = (steps and NextStep.Renew.id) > 0,
            upgrade = (steps and NextStep.Upgrade.id) > 0
    )
}

data class MemberStatus(
    val reminder: String?, // Remind upon expiration
    val tier: String,
    val expiration: String, // Expiration time
    val autoRenew: Boolean,
    val payMethod: String?, // For Stripe or Apple IAP
    val renewalBtn: Boolean, // Show renewal btn for Alipay, Wechat
    val upgradeBtn: Boolean, // Show upgrade btn for Alipay, Wechat and Stripe standard edition.
    val addOnBtn: Boolean, // Purchase addon membership period.
    val reSubscribeBtn: Boolean, // Show resubscribe btn for expired.
)


fun buildMemberStatus(ctx: Context, m: Membership): MemberStatus {
    val reminder = m.remainingDays().let {
        when {
            it == null -> null
            it < 0 -> ctx.getString(R.string.member_has_expired)
            it == 0L -> ctx.getString(R.string.member_is_expiring)
            it <= 7 -> ctx.getString(R.string.member_will_expire, it)
            else -> null
        }
    }

    return MemberStatus(
        reminder = reminder,
        tier = ctx.getString(m.tierStringRes),
        expiration = m.localizeExpireDate(),
        autoRenew = m.autoRenew ?: false,
        payMethod = null,
        renewalBtn = m.canRenewViaAliWx(),
        upgradeBtn = m.canUpgrade(),
        addOnBtn = false,
        reSubscribeBtn = m.expired()
    )
}
