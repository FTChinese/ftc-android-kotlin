package com.ft.ftchinese.ui.article

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.audio.DownloadUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class PlayerViewModel(application: Application): AndroidViewModel(application) {

    private val _downloadPercent = MutableLiveData<Float>()
    val downloadPercent: LiveData<Float>
        get() = _downloadPercent

    private var coroutineScope: CoroutineScope? = null

    @ExperimentalCoroutinesApi
    fun startFlow(context: Context, uri: Uri) {
        coroutineScope?.cancel()
        val job = SupervisorJob()
        coroutineScope = CoroutineScope(Dispatchers.Main + job).apply {
            launch {
                DownloadUtil.getDownloadTracker(context)
                    .getCurrentProgressDownload(uri)
                    .collect {
                        _downloadPercent.postValue(it)
                    }
            }
        }
    }

    fun stopFlow() {
        coroutineScope?.cancel()
    }
}
