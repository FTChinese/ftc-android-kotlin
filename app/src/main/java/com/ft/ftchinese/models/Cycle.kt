package com.ft.ftchinese.models

import org.threeten.bp.LocalDate

private val cycleNames = arrayOf("month", "year")
private val cycleValue = mapOf(
        "month" to Cycle.MONTH,
        "year" to Cycle.YEAR
)

enum class Cycle {
    MONTH,
    YEAR;

    fun string(): String {
        if (ordinal >= cycleNames.size) {
            return ""
        }

        return cycleNames[ordinal]
    }

    /**
     * Calculate the end date of a cycle.
     */
    fun endDate(d: LocalDate): LocalDate {
        return when (this) {
            MONTH -> d.plusMonths(1).plusDays(1)
            YEAR -> d.plusYears(1).plusDays(1)
        }
    }

    companion object {
        fun fromString(s: String?): Cycle? {
            return cycleValue[s]
        }
    }
}