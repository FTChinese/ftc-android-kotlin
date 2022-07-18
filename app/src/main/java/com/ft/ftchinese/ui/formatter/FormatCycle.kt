package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PeriodUnit
import com.ft.ftchinese.model.ftcsubs.YearMonthDay

private fun getUnitStr(ctx: Context, unit: PeriodUnit): String {
    return when (unit) {
        PeriodUnit.Year -> ctx.getString(R.string.cycle_year)
        PeriodUnit.Month -> ctx.getString(R.string.cycle_month)
        PeriodUnit.Day -> ctx.getString(R.string.cycle_day)
    }
}

private fun formatInterval(ctx: Context, interval: com.ft.ftchinese.model.ftcsubs.PeriodInterval, recurring: Boolean): String {
    if (interval.count <= 0) {
        return ""
    }

    if (interval.count == 1 && recurring) {
        return getUnitStr(ctx, interval.unit)
    }

    val infix = if (interval.unit == PeriodUnit.Month) {
        "个"
    } else {
        ""
    }

    return "${interval.count}${infix}${getUnitStr(ctx, interval.unit)}"
}

fun formatYMD(ctx: Context, ymd: YearMonthDay, recurring: Boolean): String {
    val intervals = ymd.intervals()
    return when (intervals.size) {
        0 -> ""
        1 -> formatInterval(ctx, intervals[0], recurring)
        else -> intervals.fold("") { acc, cur ->
            acc + formatInterval(ctx, cur, false)
        }
    }
}



