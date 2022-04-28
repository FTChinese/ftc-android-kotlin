package com.ft.ftchinese.ui.settings

import android.app.Application
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.BuildConfig
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

private const val TAG = "UpdateAppViewModel"

class UpdateAppViewModel(application: Application) : AndroidViewModel(application) {

    private val releaseStore = ReleaseStore(application)
    val connectionLiveData = ConnectionLiveData(application)
    val progressLiveData = MutableLiveData(false)
    val refreshingLiveData = MutableLiveData(false)

    val newReleaseLiveData: MutableLiveData<AppRelease> by lazy {
        MutableLiveData<AppRelease>()
    }

    val toastLiveData: MutableLiveData<ToastMessage> by lazy {
        MutableLiveData<ToastMessage>()
    }

    fun loadRelease() {
        toastLiveData.value = ToastMessage.Resource(R.string.checking_latest_release)
        progressLiveData.value = true
        viewModelScope.launch {
            val cached = loadCachedRelease()
            if (cached != null && cached.versionCode > BuildConfig.VERSION_CODE) {
                newReleaseLiveData.value = cached
                progressLiveData.value = false
                return@launch
            }

            remoteLoadOrRefresh()
        }
    }

    private suspend fun remoteLoadOrRefresh() {
        val remote = loadRemoteRelease()
        progressLiveData.value = false
        refreshingLiveData.value = false

        if (remote == null) {
            toastLiveData.value = ToastMessage.Resource(R.string.release_not_found)
            return
        }

        newReleaseLiveData.value = remote

        withContext(Dispatchers.IO) {
            releaseStore.saveVersion(remote)
        }
    }

    fun refresh() {
        toastLiveData.value = ToastMessage.Resource(R.string.checking_latest_release)
        refreshingLiveData.value = true
        viewModelScope.launch {
            remoteLoadOrRefresh()
        }
    }

    fun loadDownloadId(): Long {
        val id = releaseStore.loadDownloadId()
        if (id < 0) {
            toastLiveData.value = ToastMessage.Resource(R.string.download_not_found)
        }

        return id
    }

    fun saveDownloadId(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                releaseStore.saveDownloadId(id)
            }
        }
    }

    private suspend fun loadCachedRelease(): AppRelease? {
        return try {
            withContext(Dispatchers.IO) {
                releaseStore.loadVersion()
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

    fun findDownloadUrl(cursor: Cursor, col: String) {
        viewModelScope.launch {
            val url = getStringCol(cursor, col)

            if (url == null) {
                toastLiveData.value = ToastMessage.Text("Downloaded file not found")
            }
        }
    }

    private suspend fun getStringCol(cursor: Cursor, col: String): String? {
        return try {
             withContext(Dispatchers.IO) {
                if (!cursor.moveToFirst()) {
                    cursor.close()
                    null
                } else {
                    val colIndex = cursor.getColumnIndexOrThrow(col)
                    cursor.getString(colIndex)
                }
            }
        } catch (e: Exception) {
            null
        } finally {
            cursor.close()
        }
    }
}
