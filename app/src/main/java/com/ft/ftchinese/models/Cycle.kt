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

    companion object {
        fun fromString(s: String?): Cycle? {
            return cycleValue[s]
        }
    }
}