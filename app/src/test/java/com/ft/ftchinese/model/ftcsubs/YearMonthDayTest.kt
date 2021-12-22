package com.ft.ftchinese.model.ftcsubs

import org.junit.Assert.*
import org.junit.Test
import org.threeten.bp.LocalDate

class YearMonthDayTest {
    @Test
    fun totalDays() {
        val days = YearMonthDay(
            years = 1,
            months = 0,
            days = 0
        ).totalDays()

        assertEquals("This year has 365 days", 365, days)
    }

    @Test
    fun localDate() {
        val localDate = LocalDate.now()

        println("dayOfYear ${localDate.dayOfYear}")
        println("dayOfMonth ${localDate.dayOfMonth}")
        println("dayOfWeek ${localDate.dayOfWeek}")

        println("lengthOfMonth ${localDate.lengthOfMonth()}")
        println("lengthOfYear ${localDate.lengthOfYear()}")
    }
}
