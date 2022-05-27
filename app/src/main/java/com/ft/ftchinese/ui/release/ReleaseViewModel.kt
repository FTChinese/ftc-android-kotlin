package com.ft.ftchinese.ui.release

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.ReleaseRepo
import com.ft.ftchinese.store.ReleaseStore
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.viewmodel.BaseAppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ReleaseViewModel"

class ReleaseViewModel(application: Application) : BaseAppViewModel(application) {

    private val releaseStore = ReleaseStore(application)

    val newReleaseLiveData: MutableLiveData<AppRelease> by lazy {
        MutableLiveData<AppRelease>()
    }

    val downloadStatus: MutableLiveData<DownloadStatus> by lazy {
        MutableLiveData<DownloadStatus>()
    }

    fun loadRelease() {

        progressLiveData.value = true

        viewModelScope.launch {
            val cached = loadCachedRelease()
            if (cached != null) {
                if (cached.isNew && cached.isValid) {
                    newReleaseLiveData.value = cached
                    progressLiveData.value = false
                    return@launch
                } else {
                    releaseStore.clear()
                }
            }

            val remote = ReleaseRepo.asyncGetLatest()
            progressLiveData.value = false

            when (remote) {
                is FetchResult.LocalizedError -> {
                    toastLiveData.value = ToastMessage.Resource(remote.msgId)
                }
                is FetchResult.TextError -> {
                    toastLiveData.value = ToastMessage.Text(remote.text)
                }
                is FetchResult.Success -> {
                    newReleaseLiveData.value = remote.data
                    withContext(Dispatchers.IO) {
                        releaseStore.saveLatest(remote.data)
                    }
                }
            }
        }
    }

    fun loadDownloadId(): Long? {
        val pair = releaseStore.loadDownloadId() ?: return null
        if (pair.first < 0) {
            toastLiveData.value = ToastMessage.Resource(R.string.download_not_found)
            return null
        }

        return pair.first
    }

    fun downloadStart(id: Long, release: AppRelease) {
        downloadStatus.value = DownloadStatus.Progress

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                releaseStore.saveDownloadId(id, release)
            }
        }
    }

    fun downloadCompleted() {
        downloadStatus.value = DownloadStatus.Completed
    }

    private suspend fun loadCachedRelease(): AppRelease? {
        return try {
            withContext(Dispatchers.IO) {
                releaseStore.loadLatest()
            }
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            null
        }
    }

}
