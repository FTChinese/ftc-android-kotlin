package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PeriodUnit

data class PeriodInterval(
    val count: Int,
    val unit: PeriodUnit,
) {
    private fun getUnitStr(ctx: Context): String {
        return when (unit) {
            PeriodUnit.Year -> ctx.getString(R.string.cycle_year)
            PeriodUnit.Month -> ctx.getString(R.string.cycle_month)
            PeriodUnit.Day -> ctx.getString(R.string.cycle_day)
        }
    }

    fun format(ctx: Context, recurring: Boolean): String {
        if (count <= 0) {
            return ""
        }

        if (count == 1 && recurring) {
            return getUnitStr(ctx)
        }

        val infix = if (unit == PeriodUnit.Month) {
            "个"
        } else {
            ""
        }

        return "${count}${infix}${getUnitStr(ctx)}"
    }
}
