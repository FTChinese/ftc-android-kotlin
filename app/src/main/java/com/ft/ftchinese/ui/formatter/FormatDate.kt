package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.AutoRenewMoment
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime

fun formatYearMonthDate(ctx: Context, date: ZonedDateTime): String {
    return ctx.getString(
        R.string.formatter_year_month_date,
        date.year,
        date.monthValue,
        date.dayOfMonth,
    )
}

fun formatYearMonthDate(ctx: Context, date: LocalDate): String {
    return ctx.getString(
        R.string.formatter_year_month_date,
        date.year,
        date.monthValue,
        date.dayOfMonth,
    )
}

fun formatMoment(ctx: Context, moment: AutoRenewMoment): String {
    return if (moment.month != null) {
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
}

fun formatRedeemPeriod(ctx: Context, start: ZonedDateTime?, end: ZonedDateTime?): String {

    return ctx.getString(
        R.string.redeem_period,
        start?.let {
            formatYearMonthDate(ctx, it)
        } ?: "",
        end?.let {
            formatYearMonthDate(ctx, it)
        } ?: "",
    )
}

fun formatCouponEnjoyed(ctx: Context, date: ZonedDateTime): String {
    return ctx.getString(
        R.string.coupon_enjoyed,
        formatYearMonthDate(ctx, date)
    )
}
