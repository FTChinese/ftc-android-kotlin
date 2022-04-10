package com.ft.ftchinese.model.splash

import android.util.Log
import com.ft.ftchinese.model.enums.Tier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "Schedule"

@Serializable
data class ScheduleMeta(
    val title: String,
    val description: String,
    val theme: String,
    val adid: String,
    val sponsorMobile: String,
    @SerialName("fileTime")
    val lastModified: Long,
    val hideAd: String,
    val audiencePixelTag: String,
    val guideline: String
)

@Serializable
class Schedule(
    val meta: ScheduleMeta,
    val sections: List<ScreenAd>
)  {

    /**
     * Filter schedules ad list and keep those set on today.
     */
    fun findToday(tier: Tier?): TodayAds {

        val today = LocalDate.now()
        val tierStr = tier?.toString() ?: "free"

        val todayAds = sections
            .filter {
                it.android == "yes" && it.scheduledOn.isNotEmpty()
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
                        e.message?.let { msg -> Log.i(TAG, msg) }
                        false
                    }
                }
            }

        return TodayAds(date = today, items = todayAds)
    }
}
