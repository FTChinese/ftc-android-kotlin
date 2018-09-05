package com.ft.ftchinese.models

import android.content.Context
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.experimental.async
import android.net.Uri
import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat

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
        val weight: String
) {
    val scheduledOn: List<String>
        get() = dates.split(",")

    val imageName: String
        get() {
            val uri = Uri.parse(imageUrl)
            val segments = uri.pathSegments
            return segments[segments.size - 1]
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

        suspend fun getData(): LaunchSchedule? {
            return try {
                val job = async {
                    Fetch().get("https://api003.ftmailbox.com/index.php/jsapi/applaunchschedule")
                            .end()
                }

                val response = job.await()

                val body = response.body()?.string()

                gson.fromJson<LaunchSchedule>(body, LaunchSchedule::class.java)

            } catch (e: Exception) {
                e.printStackTrace()

                null
            }
        }
    }
}