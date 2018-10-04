package com.ft.ftchinese.models

import android.content.Context
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.experimental.async
import android.net.Uri
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Store
import com.koushikdutta.ion.Ion
import kotlinx.coroutines.experimental.Deferred
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.util.Pair
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import java.io.File

data class LaunchMeta(
    val title: String,
    val description: String,
    val theme: String,
    val adid: String,
    val sponsorMobile: String,
    val fileTime: Long
)

data class LaunchAd(
        @SerializedName("fileName") val imageUrl: String,
        @SerializedName("click") val linkUrl: String,
        @SerializedName("impression_1") val impressionUrl: String,
        val iphone: String,
        val android: String,
        val ipad: String,
        val dates: String,
        // weight actually means https://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution
        val weight: String
) : AnkoLogger {
    val scheduledOn: List<String>
        get() = dates.split(",")

    val imageName: String
        get() {
            val uri = Uri.parse(imageUrl)
            val segments = uri.pathSegments
            return segments[segments.size - 1]
        }

    fun cacheImage(context: Context) {
        if (Store.exists(context, imageName)) {
            return
        }

        Ion.with(context)
                .load(imageUrl)
                .write(File(context.filesDir, imageName))
                .setCallback { e, result ->
                    info("Download complete: ${result.absolutePath}")
                }
    }
}

class LaunchSchedule(
        val meta: LaunchMeta,
        val sections: Array<LaunchAd>
) {

    fun save(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREF_AD_SCHEDULE, Context.MODE_PRIVATE)

        // Only save data if remote is new than current one.
        val lastModified = sharedPreferences.getLong(PREF_KEY_LASTED_MODIFIED, 0)
        if (meta.fileTime <= lastModified) {
            return
        }

        val prefSchedule = formatData()

        val editor = sharedPreferences.edit()
        editor.putLong(PREF_KEY_LASTED_MODIFIED, meta.fileTime)

        prefSchedule.forEach { (key, value) ->
            val strSet = value.map {
                gson.toJson(it)
            }.toSet()

            editor.putStringSet(key, strSet)
        }

        editor.apply()
    }

    /**
     * Use date as key. For example: 20180906
     */
    fun formatData(): Map<String, Set<LaunchAd>> {
        val prefSchedule = mutableMapOf<String, MutableSet<LaunchAd>>()

        val today = LocalDate.now()

        for (adItem in sections) {
            if (adItem.android != "yes") {
                continue
            }
            for (date in adItem.scheduledOn) {
                if (date.isNullOrBlank()) {
                    continue
                }
                // If date >= today and android == "yes"
                // Add adItem to map if using date as key if not exists
                // else append to key `date`
                // Throws IllegalArgumentException
                try {
                    val planned = LocalDate.parse(date, ISODateTimeFormat.basicDate())
                    // Record those equal to or later than today.
                    if (!today.isAfter(planned)) {
                        if (prefSchedule.containsKey(date)) {
                            prefSchedule[date]?.add(adItem)
                        } else {
                            prefSchedule[date] = mutableSetOf(adItem)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    continue
                }
            }
        }

        return prefSchedule
    }

    companion object {
        const val PREF_AD_SCHEDULE = "ad_schedule"
        private const val PREF_KEY_LASTED_MODIFIED = "last_modified"

        /**
         * Download latest ad schedule upon app launch
         */
        fun fetchDataAsync(): Deferred<LaunchSchedule?> = async {
            try {
                val response = Fetch().get(NextApi.APP_LAUNCH)
                        .end()

                val body = response.body()?.string()

                gson.fromJson<LaunchSchedule>(body, LaunchSchedule::class.java)

            } catch (e: Exception) {
                e.printStackTrace()

                null
            }
        }

        /**
         * @param days specify how many days' data you want to retrieve, starting from today.
         * It returns at least today's list
         */
        fun loadFromPref(context: Context, days: Int = 0): List<LaunchAd> {
            val sharedPreferences = context.getSharedPreferences(LaunchSchedule.PREF_AD_SCHEDULE, Context.MODE_PRIVATE)

            val localDate = LocalDate.now()
            val formatter = ISODateTimeFormat.basicDate()

            val today = formatter.print(localDate)

            val ads = sharedPreferences.getStringSet(today, mutableSetOf())

            for (i in 0..days) {
                val key = formatter.print(localDate.plusDays(i))
                val adData = sharedPreferences.getStringSet(key, setOf())
                ads.union(adData)
            }

            return ads.map { gson.fromJson(it, LaunchAd::class.java) }
        }

        // Reference https://stackoverflow.com/questions/9330394/how-to-pick-an-item-by-its-probability
        // We use Apache Math library http://commons.apache.org/proper/commons-math/userguide/distribution.html
        fun randomAdFileName(context: Context): LaunchAd? {
            val candidates = loadFromPref(context)
            if (candidates.isEmpty()) return null

            val pmf = candidates.map {
                Pair(it, it.weight.toDouble())
            }.toMutableList()

            val distribution = EnumeratedDistribution(pmf)

            return distribution.sample()
        }
    }
}