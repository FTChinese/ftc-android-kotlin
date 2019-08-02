package com.ft.ftchinese.model

import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.temporal.ChronoUnit

class MembershipTest {
    @Test fun remainingDays() {
        val diff = LocalDate.now().until(LocalDate.of(2020, Month.JULY, 4), ChronoUnit.DAYS)

        println(diff)
    }
}
