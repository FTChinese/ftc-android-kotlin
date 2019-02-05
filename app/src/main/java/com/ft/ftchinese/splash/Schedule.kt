package com.ft.ftchinese.splash

import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

class Schedule(
        val meta: ScheduleMeta,
        private val sections: Array<ScreenAd>
) : AnkoLogger {

    /**
     * Filter schedules ad list and keep those set on today.
     */
    fun findToday(): ScheduleToday {
        val adItems = mutableListOf<ScreenAd>()
        val today = LocalDate.now()
        for (item in sections) {
            if (item.android != "yes") {
                continue
            }
            for (dateStr in item.scheduledOn) {
                if (dateStr.isBlank()) {
                    continue
                }

                try {
                    val date = LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE)
                    if (today.isEqual(date)) {
                        adItems.add(item)
                    }
                } catch (e: Exception) {
                    info(e.message)
                    continue
                }
            }
        }

        return ScheduleToday(date = today, items = adItems.toList())
    }
}