package com.ft.ftchinese.ui.member

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.AutoRenewMoment
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.reader.Membership

data class AutoRenewStatus(
    val date: String?, // Only exists when auto renew is on
    val on: Boolean,
    val activate: Boolean // If auto renew is off, only stripe can be activated.
)

data class SubsStatus(
    val reminder: String? = null,
    val productName: String,
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
                        details = listOf(
                            Pair(ctx.getString(R.string.label_expiration_date), m.localizeExpireDate())
                        ),
                    )
                }
                PayMethod.STRIPE, PayMethod.APPLE -> {
                    val brand = ctx.getString(m.payMethod.stringRes)
                    val expired = m.expired()
                    val reminder =  when {
                        expired -> ctx.getString(R.string.member_has_expired)
                        m.isInvalidStripe() -> ctx.getString(R.string.member_status_invalid)
                        else -> null
                    }
                    // Cancelled and expired
                    if (!m.autoRenew) {
                        SubsStatus(
                            reminder = reminder,
                            productName = name,
                            details = listOf(
                                Pair(ctx.getString(R.string.label_subs_source), brand),
                                Pair(ctx.getString(R.string.label_expiration_date), m.localizeExpireDate()),
                                Pair(ctx.getString(R.string.label_auto_renew), ctx.getString(R.string.auto_renew_off)),
                            ),
                            reactivateStripe = m.payMethod == PayMethod.STRIPE && !expired
                        )
                    } else {
                        SubsStatus(
                            reminder = reminder,
                            productName = name,
                            details = listOf(
                                Pair(ctx.getString(R.string.label_subs_source), brand),
                                Pair("自动续订", m.autoRenewMoment?.let {
                                    formatAutoRenewDate(ctx, it)
                                } ?: ""),
                            ),
                        )
                    }
                }
                PayMethod.B2B -> SubsStatus(
                    reminder = "企业订阅续订或升级请联系所属机构的管理人员",
                    productName = name,
                    details = listOf(
                        Pair(ctx.getString(R.string.label_subs_source), ctx.getString(R.string.pay_brand_b2b)),
                        Pair(ctx.getString(R.string.label_expiration_date), m.localizeExpireDate())
                    ),
                )
                else -> SubsStatus(
                    reminder = "Unknown Subscription Source",
                    productName = name,
                    details = listOf(),
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



