package com.ft.ftchinese.model.splash

import com.beust.klaxon.Json
import com.ft.ftchinese.model.order.Tier
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

data class ScheduleMeta(

        val title: String,
        val description: String,
        val theme: String,
        val adid: String,
        val sponsorMobile: String,
        @Json(name = "fileTime")
        val lastModified: Long,
        val hideAd: String,
        val audiencePixelTag: String,
        val guideline: String
)

class Schedule(
        val meta: ScheduleMeta,
        val sections: Array<ScreenAd>
) : AnkoLogger {

    /**
     * Filter schedules ad list and keep those set on today.
     */
    fun findToday(tier: Tier?): TodayAds {

        val today = LocalDate.now()
        val tierStr = tier?.toString() ?: "free"

        val todayAds = sections
                .filter {
                    it.android == "yes" && !it.scheduledOn.isEmpty()
                }
                .filter {
                    if (it.targetUser == "all") {
                        true
                    } else {
                        it.targetUser == tierStr
                    }
                }
                .filter {
                    it.scheduledOn.any {
                        try {
                            val date = LocalDate.parse(it, DateTimeFormatter.BASIC_ISO_DATE)

                            today.isEqual(date)
                        } catch (e: Exception) {
                            info(e.message)
                            false
                        }
                    }
                }

        return TodayAds(date = today, items = todayAds)
    }
}
