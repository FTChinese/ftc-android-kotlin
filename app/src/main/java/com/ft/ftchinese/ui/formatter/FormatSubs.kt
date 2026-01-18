package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.reader.Membership

object FormatSubs {

    fun rowExpiration(ctx: Context, date: String): Pair<String, String> {
        return Pair(
            ctx.getString(R.string.label_expiration_date),
            date
        )
    }

    fun rowSubsSource(ctx: Context, pm: PayMethod): Pair<String, String> {
        return Pair(
            ctx.getString(R.string.label_subs_source),
            FormatHelper.getPayMethod(ctx, pm)
        )
    }

    fun rowAutoRenewOn(ctx: Context): Pair<String, String> {
        return Pair(
            ctx.getString(R.string.label_auto_renew),
            ctx.getString(R.string.auto_renew_on)
        )
    }

    fun rowNextRenewDate(ctx: Context, m: Membership): Pair<String, String> {
        val value = m.expireDate?.let { formatYearMonthDate(ctx, it) } ?: ""
        return Pair(
            ctx.getString(R.string.label_next_renew_date),
            value
        )
    }

    fun rowRenewCycle(ctx: Context, m: Membership): Pair<String, String> {
        val value = m.cycle?.let { ctx.getString(it.stringRes) } ?: ""
        return Pair(
            ctx.getString(R.string.label_renew_cycle),
            value
        )
    }

    fun rowAutoRenewOff(ctx: Context): Pair<String, String> {
        return Pair(
            ctx.getString(R.string.label_auto_renew),
            ctx.getString(R.string.auto_renew_off)
        )
    }
}
