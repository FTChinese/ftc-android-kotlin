package com.ft.ftchinese.model.splash

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.fetch.formatLocalDate
import com.ft.ftchinese.model.fetch.parseLocalDate
import org.jetbrains.anko.AnkoLogger
import org.threeten.bp.LocalDate

const val SPLASH_SCHEDULE_FILE = "splash_schedule.json"

private const val SPLASH_AD_PREF_NAME = "splash_ad"
private const val PREF_TYPE = "type"
private const val PREF_TITLE = "title"
private const val PREF_IMAGE_URL = "image_url"
private const val PREF_LINK_URL = "link_url"
private const val PREF_IMPRESSION_URL_1 = "impression_url_1"
private const val PREF_IMPRESSION_URL_2 = "impression_url_2"
private const val PREF_IMPRESSION_URL_3 = "impression_url_3"
private const val PREF_TARGET_USER = "target_user"
private const val PREF_SCHEDULED_ON = "scheduled_on"
private const val PREF_WEIGHT = "weight"
private const val PREF_DATE = "date"

class SplashScreenManager(context: Context) : AnkoLogger {
//    private val filesDir = context.filesDir
    private val sharedPreferences = context.getSharedPreferences(SPLASH_AD_PREF_NAME, Context.MODE_PRIVATE)

    /**
     * Save ScreenAd to be used upon app launch next time.
     */
    fun save(ad: ScreenAd, date: LocalDate) {
        sharedPreferences.edit {
            putString(PREF_TYPE, ad.type)
            putString(PREF_TITLE, ad.title)
            putString(PREF_IMAGE_URL, ad.imageUrl)
            putString(PREF_LINK_URL, ad.linkUrl)
            putString(PREF_IMPRESSION_URL_1, ad.impressionUrl1)
            putString(PREF_IMPRESSION_URL_2, ad.impressionUrl2)
            putString(PREF_IMPRESSION_URL_3, ad.impressionUrl3)
            putString(PREF_TARGET_USER, ad.targetUser)
            putStringSet(PREF_SCHEDULED_ON, ad.scheduledOn.toSet())
            putString(PREF_WEIGHT, ad.weight)
            putString(PREF_DATE, formatLocalDate(date))
        }
    }

    /**
     * Load a single ScreenAd saved last time.
     */
    fun load(): ScreenAd? {

        val imageUrl = sharedPreferences.getString(PREF_IMAGE_URL, null) ?: return null

        val dateStr = sharedPreferences.getString(PREF_DATE, "")

        val ad = ScreenAd(
                type = sharedPreferences.getString(PREF_TYPE, "")
                        ?: "",
                title = sharedPreferences.getString(PREF_TITLE, "")
                        ?: "",
                imageUrl = imageUrl,
                linkUrl = sharedPreferences.getString(PREF_LINK_URL, "")
                        ?: "",
                impressionUrl1 = sharedPreferences.getString(PREF_IMPRESSION_URL_1, "")
                        ?: "",
                impressionUrl2 = sharedPreferences.getString(PREF_IMPRESSION_URL_2, "")
                        ?: "",
                impressionUrl3 = sharedPreferences.getString(PREF_IMPRESSION_URL_3, "")
                        ?: "",
                iphone = "",
                android = "",
                ipad = "",
                targetUser = sharedPreferences.getString(PREF_TARGET_USER, null),
                dates = sharedPreferences.getStringSet(PREF_SCHEDULED_ON, setOf())?.joinToString()
                        ?: "",
                weight = sharedPreferences.getString(PREF_WEIGHT, "")
                        ?: ""
        )

        ad.date = parseLocalDate(dateStr)

        return ad
    }
}
