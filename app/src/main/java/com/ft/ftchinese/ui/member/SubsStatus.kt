package com.ft.ftchinese.ui.member

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.AutoRenewMoment
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.reader.Membership

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
            if (m.vip) {
                return SubsStatus(
                    productName = ctx.getString(R.string.tier_vip),
                    details = listOf(
                        Pair(ctx.getString(R.string.label_expiration_date), ctx.getString(R.string.vip_no_expiration))
                    ),
                    addOns = listOf(),
                )
            }

            val productTitle = ctx.getString(m.tierStringRes)

            // List addon
            val addOns = m.addOns.map {
                Pair(ctx.getString(it.first.stringRes), "${it.second}天")
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
                        details = listOf(Pair(ctx.getString(R.string.label_expiration_date), m.localizeExpireDate())),
                        addOns = addOns,
                    )
                }
                PayMethod.STRIPE, PayMethod.APPLE -> {
                    val brand = ctx.getString(m.payMethod.stringRes)
                    val expired = m.expired
                    val reminder =  when {
                        expired -> ctx.getString(R.string.member_has_expired)
                        m.status?.isInvalid() == true -> ctx.getString(R.string.member_status_invalid)
                        else -> null
                    }

                    if (m.autoRenew) {
                        SubsStatus(
                            reminder = reminder,
                            productName = productTitle,
                            details = listOf(
                                Pair(ctx.getString(R.string.label_subs_source), brand),
                                Pair("自动续订", m.autoRenewMoment?.let {
                                    formatAutoRenewDate(ctx, it)
                                } ?: ""),
                            ),
                            addOns = addOns,
                        )
                    } else {
                        SubsStatus(
                            reminder = reminder,
                            productName = productTitle,
                            details = listOf(
                                Pair(ctx.getString(R.string.label_subs_source), brand),
                                Pair(ctx.getString(R.string.label_expiration_date), m.localizeExpireDate()),
                                Pair(ctx.getString(R.string.label_auto_renew), ctx.getString(R.string.auto_renew_off)),
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
                        Pair(ctx.getString(R.string.label_subs_source), ctx.getString(R.string.pay_brand_b2b)),
                        Pair(ctx.getString(R.string.label_expiration_date), m.localizeExpireDate())
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

fun formatAutoRenewDate(ctx: Context, moment: AutoRenewMoment): String {
    val monthDate =  if (moment.month != null) {
        ctx.getString(
            R.string.formatter_month_date,
            moment.month,
            moment.date
        )
    } else {
        ctx.getString(
            R.string.formatter_date,
            moment.date
        )
    }

    return ctx.getString(
        R.string.formatter_edition,
        monthDate,
        ctx.getString(moment.cycle.stringRes)
    )
}



