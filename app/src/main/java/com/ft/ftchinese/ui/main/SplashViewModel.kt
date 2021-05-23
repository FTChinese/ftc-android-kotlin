package com.ft.ftchinese.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.splash.ScreenAd
import com.ft.ftchinese.model.splash.SplashScreenManager
import com.ft.ftchinese.repository.AdClient
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.InputStream
import java.util.*

class SplashViewModel : BaseViewModel(), AnkoLogger {

    val shouldExit = MutableLiveData(false)
    val adLoaded = MutableLiveData<ScreenAd>()
    // The input stream of image and its name.
    val imageLoaded: MutableLiveData<Pair<InputStream, String>> by lazy {
        MutableLiveData<Pair<InputStream, String>>()
    }

    // Tracking the tracking of sending impression result.
    // Pair.first indicates success/failure.
    // Pair.second is the url sent to.
    val impressionResult: MutableLiveData<Pair<Boolean, String>> by lazy {
        MutableLiveData<Pair<Boolean, String>>()
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

    fun loadAd(store: SplashScreenManager, cache: FileCache) {

        viewModelScope.launch {

            val splashAd = store.load()

            if (splashAd == null) {
                info("Splash not found. Exit")
                shouldExit.value = true
                return@launch
            }

            info("Splash found $splashAd")
            if (!splashAd.isToday()) {
                info("Splash is not targeting today. Exit")
                shouldExit.value = true
                return@launch
            }

            val imageName = splashAd.imageName
            if (imageName == null) {
                info("Splash has no image name. Exit")
                shouldExit.value = true
                return@launch
            }
            val fis = withContext(Dispatchers.IO) {
                cache.readBinaryFile(imageName)
            }

            if (fis == null) {
                info("Splash image not read $imageName")
                shouldExit.value = true
                return@launch
            }

            imageLoaded.value = Pair(fis, imageName)
            adLoaded.value = splashAd!!
        }
    }

    fun sendImpression(url: String) {
        viewModelScope.launch {
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
}
