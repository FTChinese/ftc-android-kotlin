package com.ft.ftchinese.ui.subs.mysubs

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.formatter.FormatSubs

/**
 * SubsStatus shows an overview of user's current membership.
 */
data class SubsStatus(
    val reminder: String? = null, // Remind user action should be taken, e.g. renew, expired, etc..
    val productName: String, // Which product user has been subscribed to.
    // Show a list of two-column data:
    // |订阅方式           苹果App内购|
    // |自动续订           2月13日/年｜
    // For monthly edition the the last line would be: 自动续订    13日/月
    // For Stripe with auto renewal off, it looks like:
    // | 订阅方式           Stripe订阅 ｜
    // ｜期限              2022-03-01 ｜
    // | 自动续订            已关闭    ｜
    //                    打开自动续订
    val details: List<Pair<String, String>>,
    val reactivateStripe: Boolean = false,
    val addOns: List<Pair<String, String>>,
) {

    companion object {
        // The membership should be the normalized version.
        @JvmStatic
        fun newInstance(ctx: Context, m: Membership): SubsStatus {
            if (m.tier == null) {
                return SubsStatus(
                    productName = ctx.getString(R.string.tier_free),
                    details = listOf(),
                    addOns = listOf()
                )
            }

            if (m.vip) {
                return SubsStatus(
                    productName = ctx.getString(R.string.tier_vip),
                    details = listOf(
                        FormatSubs.rowExpiration(ctx, m.localizeExpireDate(ctx))
                    ),
                    addOns = listOf(),
                )
            }

            val productTitle = FormatHelper.getTier(ctx, m.tier)

            // List addon
            val addOns = m.addOns.map {
                Pair(FormatHelper.getTier(ctx, it.first), "${it.second}天")
            }

            return when (m.payMethod) {
                PayMethod.ALIPAY, PayMethod.WXPAY -> {
                    SubsStatus(
                        reminder = m.remainingDays().let {
                            when {
                                it == null -> null
                                it < 0 -> ctx.getString(R.string.member_has_expired)
                                it == 0L -> ctx.getString(R.string.member_is_expiring)
                                it <= 7 -> ctx.getString(R.string.member_will_expire, it)
                                else -> null
                            }
                        },
                        productName = productTitle,
                        details = listOf(FormatSubs.rowExpiration(ctx, m.localizeExpireDate(ctx))),
                        addOns = addOns,
                    )
                }
                PayMethod.STRIPE, PayMethod.APPLE -> {

                    val expired = m.expired
                    val reminder =  when {
                        expired -> ctx.getString(R.string.member_has_expired)
                        m.status?.isInvalid() == true -> ctx.getString(R.string.member_status_invalid)
                        m.isTrialing -> ctx.getString(R.string.sub_status_trialing)
                        else -> null
                    }

                    if (m.autoRenew) {
                        SubsStatus(
                            reminder = reminder,
                            productName = productTitle,
                            details = listOf(
                                FormatSubs.rowSubsSource(ctx, m.payMethod),
                                FormatSubs.rowAutoRenewOn(ctx),
                                FormatSubs.rowNextRenewDate(ctx, m),
                                FormatSubs.rowRenewCycle(ctx, m),
                            ),
                            addOns = addOns,
                        )
                    } else {
                        SubsStatus(
                            reminder = reminder,
                            productName = productTitle,
                            details = listOf(
                                FormatSubs.rowSubsSource(ctx, m.payMethod),
                                FormatSubs.rowExpiration(ctx, m.localizeExpireDate(ctx)),
                                FormatSubs.rowAutoRenewOff(ctx),
                            ),
                            reactivateStripe = m.payMethod == PayMethod.STRIPE && !expired,
                            addOns = addOns,
                        )
                    }
                }
                PayMethod.B2B -> SubsStatus(
                    reminder = "企业订阅续订或升级请联系所属机构的管理人员",
                    productName = productTitle,
                    details = listOf(
                        FormatSubs.rowSubsSource(ctx, m.payMethod),
                        FormatSubs.rowExpiration(ctx, m.localizeExpireDate(ctx))
                    ),
                    addOns = addOns,
                )
                else -> SubsStatus(
                    reminder = "订阅方式缺失",
                    productName = productTitle,
                    details = addOns,
                    addOns = addOns
                )
            }
        }
    }
}
