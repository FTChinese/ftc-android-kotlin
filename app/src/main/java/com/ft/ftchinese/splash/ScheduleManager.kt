package com.ft.ftchinese.splash

import android.content.Context
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.models.Tier
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.json
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.util.Pair
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.threeten.bp.LocalDate
import java.io.File

private const val apiUrl = "https://api003.ftmailbox.com/index.php/jsapi/applaunchschedule"

class ScheduleManager(context: Context) : AnkoLogger {
    private val filesDir = context.filesDir
    private val scheduleFileName = "splash_schedule.json"
    private val todayFileName = "splash_today.json"

    /**
     * Crawl the ad schedule for splash screen and cache, then extract those arranged
     * for today and cache it, then download all images for today's schedule.
     */
    fun crawl() {
        val sch = downloadSchedule() ?: return
        val ads = cacheTodaySchedule(sch)
        downloadImages(ads)
    }

    /**
     * Download and save the entire ad schedule.
     */
    private fun downloadSchedule(): Schedule? {
        return try {
            val body = Fetch().get(apiUrl)
                    .responseString() ?: return null

            File(filesDir, scheduleFileName).writeText(body)

            Klaxon().parse<Schedule>(body)
        } catch (e: Exception) {
            info(e.message)
            null
        }
    }

    private fun cacheTodaySchedule(sch: Schedule): List<ScreenAd> {
        val scheduleToday = sch.findToday()
        try {
            File(filesDir, todayFileName).writeText(json.toJsonString(scheduleToday))
        } catch (e: Exception) {
            info(e.message)
        }

        return scheduleToday.items
    }

    /**
     * Download images for today's ads
     */
    private fun downloadImages(adItems: List<ScreenAd>) {
        for (ad in adItems) {
            try {
                val file = File(filesDir, ad.imageName)
                if (file.exists()) {
                    continue
                }
                Fetch().get(ad.imageUrl).download(file)
            } catch (e: Exception) {
                info(e.message)
            }
        }
    }

    /**
     * Reference https://stackoverflow.com/questions/9330394/how-to-pick-an-item-by-its-probability
     *
     * We use Apache Math library http://commons.apache.org/proper/commons-math/userguide/distribution.html
     */
    fun pickRandomAd(tier: Tier?): ScreenAd? {
        val file = File(filesDir, todayFileName)
        if (!file.exists()) {
            return null
        }
        try {
            val body = file.readText()
            val scheduleToday = json.parse<ScheduleToday>(body) ?: return null
            val today = LocalDate.now()
            if (!today.isEqual(scheduleToday.date)) {
                return null
            }

            if (scheduleToday.items.isEmpty()) {
                return null
            }

            // Create probability mass function enumerated as a list of <T, probability>
            val pmf = scheduleToday.items.map {
                Pair(it, it.weight.toDouble())
            }.toMutableList()

            val distribution = EnumeratedDistribution(pmf)

            val screenAd = distribution.sample()

            if (screenAd.targetUser == "all") {
                return screenAd
            }

            val tierStr = tier?.string() ?: "free"

            if (tierStr != screenAd.targetUser) {
                return null
            }

            return screenAd
        } catch (e: Exception) {
            info(e.message)

            return null
        }
    }
}