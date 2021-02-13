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
    val expirationDate: String?, // Only exists for non-auto renewal.
    val brandName: String?, // Only exists for subscription mode.
    val autoRenewStatus: AutoRenewStatus?
) {
    val autoRenewOn: Boolean
        get() = autoRenewStatus != null && autoRenewStatus.on

    val autoRenewOff: Boolean
        get() = autoRenewStatus != null && !autoRenewStatus.on

    val canReactivate: Boolean
        get() = autoRenewOff && autoRenewStatus?.activate == true

    companion object {
        @JvmStatic
        fun newInstance(ctx: Context, m: Membership): SubsStatus {
            if (m.vip) {
                return SubsStatus(
                    productName = ctx.getString(R.string.tier_vip),
                    expirationDate = ctx.getString(R.string.vip_no_expiration),
                    brandName = null,
                    autoRenewStatus = null,
                )
            }

            val name = ctx.getString(m.tierStringRes)
            when (m.payMethod) {
                PayMethod.ALIPAY, PayMethod.WXPAY -> {
                    return SubsStatus(
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
                        expirationDate = m.localizeExpireDate(),
                        brandName = null,
                        autoRenewStatus = null
                    )
                }
                PayMethod.STRIPE, PayMethod.APPLE -> {
                    val brand = ctx.getString(m.payMethod.stringRes)
                    // Cancelled and expired
                    if (m.expired()) {
                        return SubsStatus(
                            reminder = ctx.getString(R.string.member_has_expired),
                            productName = name,
                            expirationDate = m.localizeExpireDate(),
                            brandName = brand,
                            autoRenewStatus = AutoRenewStatus(
                                date = null,
                                on = m.autoRenew,
                                activate = false,
                            )
                        )
                    }

                    val reminder =  if (m.isInvalidStripe()) {
                        ctx.getString(R.string.member_status_invalid)
                    } else {
                        null
                    }
                    // Cancelled but not expired
                    if (!m.autoRenew) {
                        return SubsStatus(
                            reminder = reminder,
                            productName = name,
                            expirationDate = m.localizeExpireDate(),
                            brandName = brand,
                            autoRenewStatus = AutoRenewStatus(
                                date = null,
                                on = m.autoRenew,
                                activate = m.payMethod == PayMethod.STRIPE,
                            )
                        )
                    }
                    // Not cancelled, not expired
                    return SubsStatus(
                        reminder = reminder,
                        productName = name,
                        expirationDate = null,
                        brandName = brand,
                        autoRenewStatus = AutoRenewStatus(
                            date = m.autoRenewMoment?.let {
                                formatAutoRenewDate(ctx, it)
                            },
                            on = m.autoRenew,
                            activate = false,
                        )
                    )
                }
                PayMethod.B2B -> {
                    return SubsStatus(
                        productName = name,
                        expirationDate = m.localizeExpireDate(),
                        brandName = ctx.getString(R.string.pay_brand_b2b),
                        autoRenewStatus = null,
                    )
                }
            }

            return SubsStatus(
                reminder = "Unknown subscription status",
                productName = name,
                expirationDate = m.localizeExpireDate(),
                brandName = m.payMethod?.let {
                    ctx.getString(it.stringRes)
                },
                autoRenewStatus = null,
            )
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



