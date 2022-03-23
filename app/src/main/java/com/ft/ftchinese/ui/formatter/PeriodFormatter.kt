package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.model.enums.PeriodUnit
import com.ft.ftchinese.model.ftcsubs.YearMonthDay

class PeriodFormatter(
    ymd: YearMonthDay,
    withSlash: Boolean = true,
) {
    private val prefix = if (withSlash) "/" else ""

    private val intervals = listOf(
        PeriodInterval(
            count = ymd.years,
            unit = PeriodUnit.Year
        ),
        PeriodInterval(
            count = ymd.months,
            unit = PeriodUnit.Month,
        ),
        PeriodInterval(
            count = ymd.days,
            unit = PeriodUnit.Day,
        )
    ).filter {
        it.count > 0
    }

    fun format(ctx: Context, recurring: Boolean): String {
        return prefix + when (intervals.size) {
            0 -> ""
            1 -> intervals[0].format(ctx, recurring)
            else -> intervals.fold("") { acc, cur ->
                acc + cur.format(ctx, false)
            }
        }
    }
}
