package com.ft.ftchinese.ui.release

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.repository.ReleaseRepo
import com.ft.ftchinese.store.ReleaseStore
import com.ft.ftchinese.ui.base.ConnectionLiveData
import com.ft.ftchinese.ui.components.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ReleaseViewModel"

class ReleaseViewModel(application: Application) : AndroidViewModel(application) {

    private val releaseStore = ReleaseStore(application)
    val connectionLiveData = ConnectionLiveData(application)
    val progressLiveData = MutableLiveData(false)

    val newReleaseLiveData: MutableLiveData<AppRelease> by lazy {
        MutableLiveData<AppRelease>()
    }

    val toastLiveData: MutableLiveData<ToastMessage> by lazy {
        MutableLiveData<ToastMessage>()
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

            val remote = loadRemoteRelease()
            progressLiveData.value = false

            if (remote == null) {
                toastLiveData.value = ToastMessage.Resource(R.string.release_not_found)
                return@launch
            }

            newReleaseLiveData.value = remote

            withContext(Dispatchers.IO) {
                releaseStore.saveLatest(remote)
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

    private suspend fun loadRemoteRelease(): AppRelease? {
        if (connectionLiveData.value != true) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return null
        }

        try {
            val resp = withContext(Dispatchers.IO) {
                ReleaseRepo.getLatest()
            }

            return resp.body
        } catch (e: APIError) {
            toastLiveData.value = if (e.statusCode == 404) {
                ToastMessage.Resource(R.string.release_not_found)
            } else {
                ToastMessage.fromApi(e)
            }
            return null
        } catch (e: Exception) {
            toastLiveData.value = ToastMessage.fromException(e)
            return null
        }
    }

}
