package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PeriodUnit
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDate
import org.threeten.bp.Period

data class PeriodInterval(
    val count: Int,
    val unit: PeriodUnit,
)

@Serializable
data class YearMonthDay(
    val years: Int = 0,
    val months: Int = 0,
    val days: Int = 0,
) {

    fun intervals(): List<PeriodInterval> {
        return listOf(
            PeriodInterval(
                count = years,
                unit = PeriodUnit.Year
            ),
            PeriodInterval(
                count = months,
                unit = PeriodUnit.Month,
            ),
            PeriodInterval(
                count = days,
                unit = PeriodUnit.Day,
            )
        ).filter {
            it.count > 0
        }
    }

    fun isZero(): Boolean {
        return arrayOf(years, months, days).none { it > 0 }
    }

    /**
     * This is not precisely a full year or month.
     * We have to keep it for backward compatible.
     */
    fun toCycle(): Cycle {
        if (years > 0) {
            return Cycle.YEAR
        }

        if (months > 0) {
            return Cycle.MONTH
        }

        return if (days >= 365) {
            Cycle.YEAR
        } else {
            Cycle.MONTH
        }
    }

    fun period(): Period {
        return Period.of(years, months, days + 1)
    }

    fun totalDays(): Int {
        val localDate = LocalDate.now()

        return years * localDate.lengthOfYear() + months * localDate.lengthOfMonth() + days
    }

    companion object {
        @JvmStatic
        fun zero(): YearMonthDay {
            return YearMonthDay(
                years = 0,
                months = 0,
                days = 0
            )
        }

        @JvmStatic
        fun of(cycle: Cycle): YearMonthDay {
            return when (cycle) {
                Cycle.YEAR -> YearMonthDay(
                    years = 1,
                    months = 0,
                    days = 0,
                )
                Cycle.MONTH -> YearMonthDay(
                    years = 0,
                    months = 1,
                    days = 0
                )
            }
        }

        @JvmStatic
        fun fromDays(days: Int): YearMonthDay {
            val years = days / 365
            val remainder = days % 365

            val months = remainder / 30

            return YearMonthDay(
                years = years,
                months = months,
                days = remainder % 30
            )
        }

        @JvmStatic
        fun ofYear(n: Int = 1): YearMonthDay {
            return YearMonthDay(
                years = n,
                months = 0,
                days = 0
            )
        }

        @JvmStatic
        fun ofMonth(n: Int = 1): YearMonthDay {
            return YearMonthDay(
                years = 0,
                months = n,
                days = 0
            )
        }

        @JvmStatic
        fun ofDay(n: Int = 1): YearMonthDay {
            return YearMonthDay(
                years = 0,
                months = 0,
                days = n
            )
        }
    }
}
