package com.ft.ftchinese.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.splash.ScreenAd
import com.ft.ftchinese.repository.AdClient
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SplashStore
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.*
import java.io.InputStream

private const val TAG = "SplashViewModel"

class SplashViewModel : BaseViewModel() {

    val shouldExit = MutableLiveData(false)
    val adLoaded: MutableLiveData<ScreenAd> by lazy {
        MutableLiveData<ScreenAd>()
    }

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
            for (i in 5 downTo 1) {
                counterLiveData.value = i
                delay(1000)
            }
            shouldExit.value = true
        }
    }

    fun stopCounting() {
        Log.i(TAG, "Stop counter")
        job?.cancel()
    }

    fun loadAd(store: SplashStore, cache: FileCache) {

        viewModelScope.launch {

            val splashAd = store.load()

            if (splashAd == null) {
                Log.i(TAG, "Splash not found. Exit")
                shouldExit.value = true
                return@launch
            }

            Log.i(TAG, "Splash found $splashAd")
            if (!splashAd.isToday()) {
                Log.i(TAG, "Splash is not targeting today. Exit")
                shouldExit.value = true
                return@launch
            }

            val imageName = splashAd.imageName
            if (imageName == null) {
                Log.i(TAG, "Splash has no image name. Exit")
                shouldExit.value = true
                return@launch
            }
            val fis = withContext(Dispatchers.IO) {
                cache.readBinaryFile(imageName)
            }

            if (fis == null) {
                Log.i(TAG, "Splash image not read $imageName")
                shouldExit.value = true
                return@launch
            }

            imageLoaded.value = Pair(fis, imageName)
            adLoaded.value = splashAd
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
                e.message?.let { Log.i(TAG, it) }
                impressionResult.value = Pair(false, url)
            }
        }
    }
}
