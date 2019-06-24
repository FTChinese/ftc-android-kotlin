package com.ft.ftchinese.model.splash

import android.net.Uri
import com.beust.klaxon.Json
import com.ft.ftchinese.util.Fetch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.threeten.bp.LocalDate
import java.io.File
import java.util.*

data class ScreenAd(
        val type: String,
        val title: String,

        @Json("fileName")
        val imageUrl: String,

        @Json("click")
        val linkUrl: String,

        @Json("impression_1")
        val impressionUrl1: String,

        @Json("impression_2")
        val impressionUrl2: String?,

        @Json("impression_3")
        val impressionUrl3: String?,

        val iphone: String,
        val android: String,
        val ipad: String,
        // targetUser is an enum: all, free, standard, premium.
        // It indicates which groups of user can see the launch ad.

        @Json("audienceCohort")
        val targetUser: String? = null,

        val dates: String,
        // weight actually means https://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution
        val weight: String
) : AnkoLogger {
    val scheduledOn: List<String>
        get() = dates.split(",")

    var date: LocalDate? = null

    fun isToday(): Boolean {
        if (date?.isEqual(LocalDate.now()) == true) {
            return true
        }

        return false
    }

    // The name used to cache image locally
    val imageName: String
        get() {
            // NOTE: Uri.parse returns null if parsing failed
            // including empty string.
            return try {
                val uri = Uri.parse(imageUrl) ?: return ""
                val segments = uri.pathSegments
                 segments[segments.size - 1]
            } catch (e: Exception) {
                ""
            }
        }

    fun downloadImage(filesDir: File) {
        try {
            val file = File(filesDir, imageName)
            if (file.exists()) {
                return
            }
            Fetch().get(imageUrl).download(file)
        } catch (e: Exception) {
            info(e)
        }
    }

    fun impressionDest(): List<String> {
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

        return urls.map {
            val urlStr = it.replace("[timestamp]", "$timestamp")
            Uri.parse(urlStr)
                    .buildUpon()
                    .appendQueryParameter("fttime", "$timestamp")
                    .build()
                    .toString()

        }
    }
}
