package com.ft.ftchinese.ui.splash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.StatsTracker
import com.ft.ftchinese.model.splash.SPLASH_SCHEDULE_FILE
import com.ft.ftchinese.model.splash.Schedule
import com.ft.ftchinese.model.splash.ScreenAd
import com.ft.ftchinese.model.splash.SplashScreenManager
import com.ft.ftchinese.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashViewModel : ViewModel() {

    val scheduleResult = MutableLiveData<Schedule>()

    fun loadSchedule(cache: FileCache, onWifi: Boolean) {
        viewModelScope.launch {
            val schedule = withContext(Dispatchers.IO) {
                val body = cache.loadText(SPLASH_SCHEDULE_FILE)

                // Cache not found
                if (body.isNullOrBlank()) {
                    if (onWifi) {
                        fetchSchedule(cache)
                    } else {
                        null
                    }
                } else {
                    // Cache found.
                    if (onWifi) {
                        launch(Dispatchers.IO) {
                            fetchSchedule(cache)
                        }
                    }

                    try {
                        Klaxon().parse<Schedule>(body)
                    } catch (e: Exception) {
                        null
                    }
                }
            } ?: return@launch

            scheduleResult.value = schedule
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

    fun prepare(sessionManager: SessionManager, splashManager: SplashScreenManager, schedule: Schedule) {
        viewModelScope.launch(Dispatchers.IO) {
            val tier = sessionManager.loadAccount()
                    ?.membership?.tier

            splashManager.prepareNextRound(schedule, tier)
        }
    }

    fun sendImpression(screenAd: ScreenAd, tracker: StatsTracker) {
        viewModelScope.launch(Dispatchers.IO) {
            screenAd.impressionDest().forEach {
                try {
                    tracker.launchAdSent(it)

                    Fetch().get(it).responseString()

                    tracker.launchAdSuccess(it)
                } catch (e: Exception) {

                    tracker.launchAdFail(it)
                }
            }
        }
    }
}
