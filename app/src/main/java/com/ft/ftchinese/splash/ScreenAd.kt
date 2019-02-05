package com.ft.ftchinese.splash

import android.net.Uri
import com.beust.klaxon.Json
import com.ft.ftchinese.util.Fetch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.File
import java.util.*

data class ScreenAd(
        val type: String,
        val title: String,
        @Json("fileName") val imageUrl: String,
        @Json("click") val linkUrl: String,
        @Json("impression_1") val impressionUrl1: String,
        @Json("impression_2") val impressionUrl2: String?,
        @Json("impression_3") val impressionUrl3: String?,
        val iphone: String,
        val android: String,
        val ipad: String,
        // targetUser is an enum: all, free, standard, premium.
        // It indicates which groups of user can see the launch ad.
        @Json("audienceCohort") val targetUser: String?,
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
        val timestamp = Date().time / 1000

        urls.forEach {
            val urlStr = it.replace("[timestamp]", "$timestamp")
            val url = Uri.parse(urlStr)
                    .buildUpon()
                    .appendQueryParameter("fttime", "$timestamp")
                    .build()
                    .toString()

            info("Send impression to $url")

            try {
                Fetch().get(url).responseString()
            } catch (e: Exception) {
                info("Error sending impression: ${e.message}")
            }

        }
    }
}