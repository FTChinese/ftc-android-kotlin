package com.ft.ftchinese.ui.launch

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.StatsTracker
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.model.splash.SPLASH_SCHEDULE_FILE
import com.ft.ftchinese.model.splash.Schedule
import com.ft.ftchinese.model.splash.ScreenAd
import com.ft.ftchinese.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*

class SplashViewModel : ViewModel(), AnkoLogger {

    val screenAdSelected = MutableLiveData<ScreenAd>()

    private fun loadSchedule(cache: FileCache, onWifi: Boolean): Schedule? {

        val body = cache.loadText(SPLASH_SCHEDULE_FILE)

        // Cache not found
        if (body.isNullOrBlank()) {
            return if (onWifi) {
                fetchSchedule(cache)
            } else {
                null
            }
        }

        // Cache found.
        return try {
            Klaxon().parse<Schedule>(body)
        } catch (e: Exception) {
            null
        } finally {
            if (onWifi) {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        fetchSchedule(cache)
                    }
                }
            }
        }
    }


    private fun fetchSchedule(cache: FileCache): Schedule? {
        try {
            val body = Fetch()
                    .get(LAUNCH_SCHEDULE_URL)
                    .responseString()

            if (body.isNullOrBlank()) {
                return null
            }

            cache.saveText(SPLASH_SCHEDULE_FILE, body)

            return Klaxon().parse<Schedule>(body)

        } catch (e: Exception) {
            return null
        }
    }


    fun prepareNextRound(cache: FileCache, onWifi: Boolean, tier: Tier?) {
        // 1. Load all schedule
        // 2. Find today's ScreenAd from the schedule.
        // 3. Pick a random ScreenAd from today's list.
        // 4. Save the selected ScreenAd.
        // 5. Download the image used by this ScreenAd
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val schedule = loadSchedule(cache, onWifi) ?: return@withContext

                val todayAds = schedule.findToday(tier)
                val screenAd = todayAds.pickRandom() ?: return@withContext

                screenAdSelected.value = screenAd

                if (!onWifi) {
                    return@withContext
                }

                try {
                    val imageBytes = Fetch()
                            .get(screenAd.imageUrl)
                            .download()
                            ?: return@withContext

                    val imageName = screenAd.imageName ?: return@withContext

                    cache.writeBinaryFile(imageName, imageBytes)
                } catch (e: Exception) {
                    info(e)
                }
            }
        }
    }

    fun sendImpression(screenAd: ScreenAd, tracker: StatsTracker) {
        viewModelScope.launch(Dispatchers.IO) {
            screenAd.impressionDest().forEach {
                val timestamp = Date().time / 1000

                val bustedUrl = Uri.parse(it.replace("[timestamp]", "$timestamp"))
                        .buildUpon()
                        .appendQueryParameter("fttime", "$timestamp")
                        .build()
                        .toString()

                try {
                    tracker.launchAdSent(it)

                    Fetch().get(bustedUrl).responseString()

                    tracker.launchAdSuccess(it)
                } catch (e: Exception) {

                    tracker.launchAdFail(it)
                }
            }
        }
    }
}
