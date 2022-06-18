package com.ft.ftchinese.model.splash

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.threeten.bp.LocalDate

@Serializable
data class ScreenAd(
    val type: String,
    val title: String,

    @SerialName("fileName")
    val imageUrl: String,

    @SerialName("click")
    val linkUrl: String,

    @SerialName("impression_1")
    val impressionUrl1: String,

    @SerialName("impression_2")
    val impressionUrl2: String?,

    @SerialName("impression_3")
    val impressionUrl3: String?,

    val iphone: String,
    val android: String,
    val ipad: String,
    // targetUser is an enum: all, free, standard, premium.
    // It indicates which groups of user can see the launch ad.

    @SerialName("audienceCohort")
    val targetUser: String? = null,

    val dates: String,
    // weight actually means https://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution
    val weight: String
) {
    val scheduledOn: List<String>
        get() = dates.split(",")

    @Transient
    var date: LocalDate? = null

    // I don't know how to play a video in an Image element.
    val isVideo: Boolean
        get() = imageUrl.endsWith(".mp4")

    fun isToday(): Boolean {
        if (date?.isEqual(LocalDate.now()) == true) {
            return true
        }

        return false
    }

    // The name used to cache image locally
    val imageName: String?
        get() {
            // NOTE: Uri.parse returns null if parsing failed
            // including empty string.
            return try {
                val uri = Uri.parse(imageUrl) ?: return ""
                val segments = uri.pathSegments
                 segments[segments.size - 1]
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Collect all impression target.
     */
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

        return urls
    }
}
