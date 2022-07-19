package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.reader.AutoRenewMoment

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

    private fun formatAutoRenewMoment(ctx: Context, moment: AutoRenewMoment): String {
        val monthDate =  formatMoment(ctx, moment)

        return ctx.getString(
            R.string.formatter_edition,
            monthDate,
            ctx.getString(moment.cycle.stringRes)
        )
    }

    fun rowAutoRenewOn(ctx: Context, mmt: AutoRenewMoment?): Pair<String, String> {
        return Pair(
            ctx.getString(R.string.label_auto_renew),
            mmt?.let {
                formatAutoRenewMoment(ctx, mmt)
            } ?: ""
        )
    }

    fun rowAutoRenewOff(ctx: Context): Pair<String, String> {
        return Pair(
            ctx.getString(R.string.label_auto_renew),
            ctx.getString(R.string.auto_renew_off)
        )
    }
}
