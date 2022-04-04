package com.ft.ftchinese.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.repository.ReleaseRepo
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.isConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateAppViewModel(application: Application) : AndroidViewModel(application) {
    private val cache = FileCache(application)

    val isNetworkAvailable = MutableLiveData<Boolean>(application.isConnected)

    val cachedReleaseFound = MutableLiveData<Boolean>()
    val releaseResult: MutableLiveData<FetchResult<AppRelease>> by lazy {
        MutableLiveData<FetchResult<AppRelease>>()
    }

    // Load current version's release log from cache.
    fun loadCachedRelease(filename: String) {
        viewModelScope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    val text = cache.loadText(filename)

                    if (text == null) {
                        null
                    } else {
                        json.parse<AppRelease>(text)
                    }
                }

                if (release != null) {
                    cachedReleaseFound.value = true

                    releaseResult.value = FetchResult.Success(release)

                    return@launch
                }

                cachedReleaseFound.value = false
            } catch (e: Exception) {
                cachedReleaseFound.value = false
            }
        }
    }

    // Fetch release log for either current version of latest version.
    fun fetchRelease(current: Boolean) {
        if (isNetworkAvailable.value != true) {
            releaseResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    if (current) {
                        ReleaseRepo.getRelease(BuildConfig.VERSION_NAME)
                    } else {
                        ReleaseRepo.getLatest()
                    }
                }

                if (resp.body == null) {
                    releaseResult.value = FetchResult.LocalizedError(R.string.release_not_found)

                    return@launch
                }

                releaseResult.value = FetchResult.Success(resp.body)

                if (resp.raw.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        cache.saveText(resp.body.cacheFileName(), resp.raw)
                    }
                }
            } catch (e: APIError) {
                releaseResult.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.release_not_found)
                } else {
                    FetchResult.fromApi(e)
                }

            } catch (e: Exception) {
                releaseResult.value = FetchResult.fromException(e)
            }
        }
    }
}
