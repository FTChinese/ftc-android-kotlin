package com.ft.ftchinese.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.splash.SPLASH_SCHEDULE_FILE
import com.ft.ftchinese.model.splash.Schedule
import com.ft.ftchinese.model.splash.ScreenAd
import com.ft.ftchinese.model.splash.SplashScreenManager
import com.ft.ftchinese.repository.AdClient
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.InputStream
import java.util.*

class SplashViewModel : BaseViewModel(), AnkoLogger {

    val screenAdSelected = MutableLiveData<ScreenAd>()

    val shouldExit = MutableLiveData(false)
    val adLoaded = MutableLiveData<ScreenAd>()
    // The input stream of image and its name.
    val imageLoaded = MutableLiveData<Pair<InputStream, String>> by lazy {
        MutableLiveData<InputStream>()
    }


    // Tracking the tracking of sending impression result.
    // Pair.first indicates success/failure.
    // Pair.second is the url sent to.
    val impressionResult: MutableLiveData<Pair<Boolean, String>> by lazy {
        MutableLiveData<Pair<Boolean, String>>()
    }

    fun loadAd(store: SplashScreenManager, cache: FileCache) {
        val splashAd = store.load()

        if (splashAd == null) {
            shouldExit.value = true
            return
        }

        if (!splashAd.isToday()) {
            shouldExit.value = true
            return
        }

        val imageName = splashAd.imageName
        if (imageName == null) {
            shouldExit.value = true
            return
        }

        viewModelScope.launch {
            val fis = withContext(Dispatchers.IO) {
                cache.readBinaryFile(imageName)
            }

            if (fis == null) {
                shouldExit.value = true
                return@launch
            }

            imageLoaded.value = Pair(fis, imageName)
            adLoaded.value = splashAd
        }
    }

    fun sendImpression(url: String) {
        viewModelScope.launch() {
            try {
                withContext(Dispatchers.IO) {
                    AdClient.sendImpression(url)
                }

                impressionResult.value = Pair(true, url)
            } catch (e: Exception) {
                info(e)
                impressionResult.value = Pair(false, url)
            }
        }
    }

    val counterLiveData = MutableLiveData<Int>()
    private var job: Job? = null

    fun startCounting() {
        job = viewModelScope.launch {
            for (i in 5 downTo 0) {
                counterLiveData.value = i
                delay(1000)
            }
            shouldExit.value = true
        }
    }

    fun stopCounting() {
        job?.cancel()
        shouldExit.value = true
    }
}
