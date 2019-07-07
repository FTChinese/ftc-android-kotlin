package com.ft.ftchinese.model.splash

import android.content.Context
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.util.formatLocalDate
import com.ft.ftchinese.util.parseLocalDate
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
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
    private val filesDir = context.filesDir
    private val sharedPreferences = context.getSharedPreferences(SPLASH_AD_PREF_NAME, Context.MODE_PRIVATE)

    fun save(ad: ScreenAd, date: LocalDate) {
        val editor = sharedPreferences.edit()

        editor.putString(PREF_TYPE, ad.type)
        editor.putString(PREF_TITLE, ad.title)
        editor.putString(PREF_IMAGE_URL, ad.imageUrl)
        editor.putString(PREF_LINK_URL, ad.linkUrl)
        editor.putString(PREF_IMPRESSION_URL_1, ad.impressionUrl1)
        editor.putString(PREF_IMPRESSION_URL_2, ad.impressionUrl2)
        editor.putString(PREF_IMPRESSION_URL_3, ad.impressionUrl3)
        editor.putString(PREF_TARGET_USER, ad.targetUser)
        editor.putStringSet(PREF_SCHEDULED_ON, ad.scheduledOn.toSet())
        editor.putString(PREF_WEIGHT, ad.weight)
        editor.putString(PREF_DATE, formatLocalDate(date))

        editor.apply()
    }

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

    fun prepareNextRound(sch: Schedule?, tier: Tier?) {
        if (sch == null) {
            return
        }

        val todayAds = sch.findToday(tier)

        info("Today's ads: $todayAds")

        val screenAd = todayAds.pickRandom() ?: return

        info("Selected a random ad")

        save(screenAd, todayAds.date)

        screenAd.downloadImage(filesDir)
    }
}
