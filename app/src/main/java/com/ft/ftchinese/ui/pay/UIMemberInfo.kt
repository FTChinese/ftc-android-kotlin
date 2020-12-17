package com.ft.ftchinese.ui.pay

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.subscription.PayMethod
import com.ft.ftchinese.model.subscription.Tier


data class MemberStatus(
    val reminder: String?, // Remind upon expiration
    val tier: String,
    val expiration: String, // Expiration time
    val autoRenewal: String?,
    val reactivateStripeBtn: Boolean,
    val payMethod: String?, // For Stripe or Apple IAP
    val renewalBtn: Boolean, // Show renewal btn for Alipay, Wechat
    val upgradeBtn: Boolean, // Show upgrade btn for Alipay, Wechat and Stripe standard edition.
    val addOnBtn: Boolean, // Purchase addon membership period.
    val reSubscribeBtn: Boolean, // Show resubscribe btn for expired.
)


fun buildMemberStatus(ctx: Context, m: Membership): MemberStatus {
    if (m.vip) {
        return MemberStatus(
            reminder = null,
            tier = ctx.getString(R.string.tier_vip),
            expiration = m.localizeExpireDate(),
            autoRenewal = null,
            reactivateStripeBtn = false,
            payMethod = null,
            renewalBtn = false,
            upgradeBtn = false,
            addOnBtn = false,
            reSubscribeBtn = false
        )
    }

    val autoRenewal = when (m.autoRenew) {
        true -> ctx.getString(R.string.auto_renew_on)
        false -> ctx.getString(R.string.auto_renew_off)
    }

    return when (m.payMethod) {
        PayMethod.ALIPAY, PayMethod.WXPAY -> {
            return MemberStatus(
                reminder = m.remainingDays().let {
                    when {
                        it == null -> null
                        it < 0 -> ctx.getString(R.string.member_has_expired)
                        it == 0L -> ctx.getString(R.string.member_is_expiring)
                        it <= 7 -> ctx.getString(R.string.member_will_expire, it)
                        else -> null
                    }
                },
                tier = ctx.getString(m.tierStringRes),
                expiration = m.localizeExpireDate(),
                autoRenewal = null,
                reactivateStripeBtn = false,
                payMethod = null,
                renewalBtn = m.canRenewViaAliWx(),
                upgradeBtn = m.canUpgrade(),
                addOnBtn = false,
                reSubscribeBtn = m.expired()
            )
        }
        PayMethod.STRIPE -> {

            val isExpired = m.expired()

            return MemberStatus(
                reminder = if (isExpired) {
                    ctx.getString(R.string.member_has_expired)
                } else {
                    if (m.isInvalidStripe()) {
                        ctx.getString(R.string.member_status_invalid)
                    } else {
                        null
                    }
                },
                tier = ctx.getString(m.tierStringRes),
                expiration = m.localizeExpireDate(),
                autoRenewal = autoRenewal,
                reactivateStripeBtn = m.canReactivateStripe(),
                payMethod = ctx.getString(R.string.subs_brand_stripe),
                renewalBtn = false,
                upgradeBtn = !isExpired && m.tier == Tier.STANDARD,
                addOnBtn = false,
                reSubscribeBtn = isExpired
            )
        }
        PayMethod.APPLE -> {
            val isExpired = m.expired()

            return MemberStatus(
                reminder = if (isExpired) {
                    ctx.getString(R.string.member_has_expired)
                } else {
                    null
                },
                tier = ctx.getString(m.tierStringRes),
                expiration = m.localizeExpireDate(),
                autoRenewal = autoRenewal,
                reactivateStripeBtn = false,
                payMethod = ctx.getString(R.string.subs_brand_apple),
                renewalBtn = false,
                upgradeBtn = false,
                addOnBtn = false,
                reSubscribeBtn = isExpired
            )
        }
        PayMethod.B2B -> {
            val isExpired = m.expired()

            return MemberStatus(
                reminder = if (isExpired) {
                    ctx.getString(R.string.member_has_expired)
                } else {
                    null
                },
                tier = ctx.getString(m.tierStringRes),
                expiration = m.localizeExpireDate(),
                autoRenewal = null,
                reactivateStripeBtn = false,
                payMethod = ctx.getString(R.string.subs_brand_b2b),
                renewalBtn = false,
                upgradeBtn = false,
                addOnBtn = false,
                reSubscribeBtn = isExpired
            )
        }
        // VIP
        else -> MemberStatus(
            reminder = null,
            tier = ctx.getString(m.tierStringRes),
            expiration = m.localizeExpireDate(),
            autoRenewal = null,
            reactivateStripeBtn = false,
            payMethod = null,
            renewalBtn = false,
            upgradeBtn = false,
            addOnBtn = false,
            reSubscribeBtn = false
        )
    }
}
