package com.ft.ftchinese.models

import android.content.Context
import com.ft.ftchinese.util.gson
import com.google.gson.annotations.SerializedName
import android.net.Uri
import com.ft.ftchinese.util.Store
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Request
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.util.Pair
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import java.io.File

private const val apiUrl = "https://api003.ftmailbox.com/index.php/jsapi/applaunchschedule"
private const val PREF_AD_SCHEDULE = "ad_schedule"
private const val PREF_KEY_LASTED_MODIFIED = "last_modified"

data class LaunchMeta(

    val title: String,
    val description: String,
    val theme: String,
    val adid: String,
    val sponsorMobile: String,
    val fileTime: Long
)

data class LaunchAd(
        val type: String,
        val title: String,
        @SerializedName("fileName") val imageUrl: String,
        @SerializedName("click") val linkUrl: String,
        @SerializedName("impression_1") val impressionUrl1: String,
        @SerializedName("impression_2") val impressionUrl2: String?,
        @SerializedName("impression_3") val impressionUrl3: String?,
        val iphone: String,
        val android: String,
        val ipad: String,
        // targetUser is an enum: all, free, standard, premium.
        // It indicates which groups of user can see the launch ad.
        @SerializedName("audienceCohort") val targetUser: String?,
        val dates: String,
        // weight actually means https://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution
        val weight: String
) : AnkoLogger {
    val scheduledOn: List<String>
        get() = dates.split(",")

    // The name used to cache image locally
    val imageName: String
        get() {
            val uri = Uri.parse(imageUrl)
            val segments = uri.pathSegments
            return segments[segments.size - 1]
        }

    // Save ad image. Returns the Request object so that
    // we can cancel it in if host activity is destroyed while downloading.
    fun cacheImage(filesDir: File): Request? {

        if (Store.exists(filesDir, imageName)) {
            return null
        }

        return Fuel.download(imageUrl)
                .destination { _, _ ->
                    File(filesDir, imageName)
                }
                .response { _, _, result ->
                    info("Download ad image complete: $result")
                }
    }

    // Notify that we successfully showed ad to user.
    fun sendImpression()  {
        val urls = mutableListOf<String>()
        if (impressionUrl1.isNotEmpty()) {
            urls.add(impressionUrl1)
        }
        if (impressionUrl2 != null && impressionUrl2.isNotEmpty()) {
            urls.add(impressionUrl2)
        }

        if (impressionUrl3 != null && impressionUrl3.isNotEmpty()) {
            urls.add(impressionUrl3)
        }
        val timestamp = DateTime.now().millis / 1000

        urls.forEach {
            val urlStr = it.replace("[timestamp]", "$timestamp")
            val url = Uri.parse(urlStr)
                    .buildUpon()
                    .appendQueryParameter("fttime", "$timestamp")
                    .build()
                    .toString()

            info("Send impression to $url")

            Fuel.get(url)
                    .responseString { _, _, result ->
                        val (_, error) = result

                        info("Send impression result: $error")
                    }
        }
    }
}

class LaunchSchedule(
        val meta: LaunchMeta,
        private val sections: Array<LaunchAd>
) : AnkoLogger {

    /**
     * Transform an array of LaunchAd into a map using date as key. For example: 20180906.
     * Value is a set of LaunchAd scheduled to be used on the date of `key`
     */
    fun transform(): Map<String, Set<LaunchAd>> {
        val prefSchedule = mutableMapOf<String, MutableSet<LaunchAd>>()

        val today = LocalDate.now()

        for (adItem in sections) {
            if (adItem.android != "yes") {
                continue
            }
            for (date in adItem.scheduledOn) {
                if (date.isBlank()) {
                    continue
                }
                // If date >= today and android == "yes"
                // Add adItem to map the key does not exist,
                // else append to key.
                // Throws IllegalArgumentException
                try {
                    val planned = LocalDate.parse(date, ISODateTimeFormat.basicDate())
                    // Record those equal to or later than today.
                    if (today.isBefore(planned)) {
                        continue
                    }

                    if (prefSchedule.containsKey(date)) {
                        prefSchedule[date]?.add(adItem)
                    } else {
                        prefSchedule[date] = mutableSetOf(adItem)
                    }
                } catch (e: Exception) {
                    info("Parse date failed: $e")
                    continue
                }
            }
        }

        return prefSchedule
    }
}

class LaunchAdManager(context: Context) : AnkoLogger {
    private val sharedPreferences = context.getSharedPreferences(PREF_AD_SCHEDULE, Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    /**
     * Download latest ad schedule upon app launch
     */
    fun fetchAndCache(): Request {

        return Fuel.get(apiUrl)
                .responseString { _, _, result ->
                    val (data, error) = result

                    if (error != null || data == null) {
                        info("Cannot get ad schedule data: $error")
                        return@responseString
                    }

                    val schedule = try {
                        gson.fromJson<LaunchSchedule>(data, LaunchSchedule::class.java)
                    } catch (e: Exception) {
                        return@responseString
                    }

                    save(schedule)
                }
    }

    private fun save(schedule: LaunchSchedule) {

        // Only save data if remote is newer than current one.
        val lastModified = sharedPreferences.getLong(PREF_KEY_LASTED_MODIFIED, 0)
        if (schedule.meta.fileTime <= lastModified) {
            return
        }

        val prefSchedule = schedule.transform()

        editor.putLong(PREF_KEY_LASTED_MODIFIED, schedule.meta.fileTime)

        prefSchedule.forEach { (key, value) ->
            val strSet = value.map {
                gson.toJson(it)
            }.toSet()

            editor.putStringSet(key, strSet)
        }

        editor.apply()
    }

    fun load(days: Int = 0): List<LaunchAd> {

        val localDate = LocalDate.now()
        val formatter = ISODateTimeFormat.basicDate()

        // Format today to yyyyMMdd.
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
    fun getRandomAd(membership: Membership?): LaunchAd? {
        val candidates = load()
        if (candidates.isEmpty()) return null

        // Create probability mass function enumerated as a list of <T, probability>
        val pmf = candidates.map {
            Pair(it, it.weight.toDouble())
        }.toMutableList()

        val distribution = EnumeratedDistribution(pmf)

        val ad = distribution.sample()

        if (membership == null || ad.targetUser == null || ad.targetUser == "all") {
            return ad
        }

        // If user is not targeted do not show the ad.
        val tier = if (membership.tier.isBlank()) "free" else membership.tier

        if (tier != ad.targetUser) {
            return null
        }

        return ad
    }

    companion object {
        private var instance: LaunchAdManager? = null

        @Synchronized fun getInstance(ctx: Context): LaunchAdManager {
            if (instance == null) {
                instance = LaunchAdManager(ctx.applicationContext)
            }

            return instance!!
        }
    }
}