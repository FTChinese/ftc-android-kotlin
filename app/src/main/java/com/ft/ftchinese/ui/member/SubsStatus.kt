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
) {
    companion object {
        @JvmStatic
        fun newInstance(ctx: Context, m: Membership): SubsStatus {
            if (m.vip) {
                return SubsStatus(
                    productName = ctx.getString(R.string.tier_vip),
                    details = listOf(
                        Pair(ctx.getString(R.string.label_expiration_date), ctx.getString(R.string.vip_no_expiration))
                    ),
                )
            }

            val name = ctx.getString(m.tierStringRes)
            val addOns = mutableListOf<Pair<String, String>>().apply {
                if (m.hasPremiumAddOn) {
                    add(Pair("高端版AddOn", "${m.premiumAddOn}天"))
                }

                if (m.hasStandardAddOn) {
                    add(Pair("标准版AddOn", "${m.standardAddOn}天"))
                }
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
                        productName = name,
                        details = mutableListOf(Pair(ctx.getString(R.string.label_expiration_date), m.localizeExpireDate())).apply {
                            addAll(addOns)
                        },
                    )
                }
                PayMethod.STRIPE, PayMethod.APPLE -> {
                    val brand = ctx.getString(m.payMethod.stringRes)
                    val expired = m.expired()
                    val reminder =  when {
                        expired && !m.hasStandardAddOn && !m.hasPremiumAddOn -> ctx.getString(R.string.member_has_expired)
                        m.isInvalidStripe() -> ctx.getString(R.string.member_status_invalid)
                        else -> null
                    }
                    // Cancelled and expired
                    if (!m.autoRenew) {
                        SubsStatus(
                            reminder = reminder,
                            productName = name,
                            details = mutableListOf(
                                Pair(ctx.getString(R.string.label_subs_source), brand),
                                Pair(ctx.getString(R.string.label_expiration_date), m.localizeExpireDate()),
                                Pair(ctx.getString(R.string.label_auto_renew), ctx.getString(R.string.auto_renew_off)),
                            ).apply {
                                addAll(addOns)
                            },
                            reactivateStripe = m.payMethod == PayMethod.STRIPE && !expired
                        )
                    } else {
                        SubsStatus(
                            reminder = reminder,
                            productName = name,
                            details = mutableListOf(
                                Pair(ctx.getString(R.string.label_subs_source), brand),
                                Pair("自动续订", m.autoRenewMoment?.let {
                                    formatAutoRenewDate(ctx, it)
                                } ?: ""),
                            ).apply {
                                addAll(addOns)
                            },
                        )
                    }
                }
                PayMethod.B2B -> SubsStatus(
                    reminder = "企业订阅续订或升级请联系所属机构的管理人员",
                    productName = name,
                    details = mutableListOf(
                        Pair(ctx.getString(R.string.label_subs_source), ctx.getString(R.string.pay_brand_b2b)),
                        Pair(ctx.getString(R.string.label_expiration_date), m.localizeExpireDate())
                    ).apply {
                        addAll(addOns)
                    },
                )
                else -> SubsStatus(
                    reminder = "Unknown Subscription Source",
                    productName = name,
                    details = addOns,
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



