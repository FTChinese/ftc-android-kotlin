package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.reader.AutoRenewMoment
import com.ft.ftchinese.model.reader.SubsTier

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

    fun rowSubsTier(ctx: Context, tier: SubsTier): Pair<String, String> {
        return Pair(
            ctx.getString(R.string.label_current_subs),
            when (tier) {
                SubsTier.Free -> ctx.getString(R.string.tier_free)
                SubsTier.Vip -> ctx.getString(R.string.tier_vip)
                SubsTier.Standard -> ctx.getString(R.string.tier_standard)
                SubsTier.Premium -> ctx.getString(R.string.tier_premium)
            }
        )
    }
}
