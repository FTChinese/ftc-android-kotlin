package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ReadingHistoryDao
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.ServerError
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.repository.ReleaseRepo
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class SettingsViewModel(
    private val cache: FileCache
) : ViewModel(), AnkoLogger {

    val isNetworkAvailable = MutableLiveData<Boolean>()

    val cacheSizeResult = MutableLiveData<String>()
    val cacheClearedResult = MutableLiveData<Boolean>()

    val articlesReadResult = MutableLiveData<Int>()
    val articlesClearedResult = MutableLiveData<Boolean>()

    val cachedReleaseFound = MutableLiveData<Boolean>()
    val releaseResult: MutableLiveData<FetchResult<AppRelease>> by lazy {
        MutableLiveData<FetchResult<AppRelease>>()
    }

    fun calculateCacheSize() {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) {
                cache.space()
            }

            cacheSizeResult.value = size
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                cache.clear()

            }

            cacheClearedResult.value = result
        }
    }

    fun countReadArticles(readDao: ReadingHistoryDao) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                readDao.count()
            }

            articlesReadResult.value = count
        }
    }

    fun truncateReadArticles(readDao: ReadingHistoryDao) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                readDao.deleteAll()
                readDao.vacuumDb(SimpleSQLiteQuery("VACUUM"))
            }

            info("Truncated $result")

            articlesClearedResult.value = true
            articlesReadResult.value = 0
        }
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
                val respData = withContext(Dispatchers.IO) {
                    if (current) {
                        ReleaseRepo.getRelease(BuildConfig.VERSION_NAME)
                    } else {
                        ReleaseRepo.getLatest()
                    }
                }

                if (respData == null) {
                    releaseResult.value = FetchResult.LocalizedError(R.string.release_not_found)

                    return@launch
                }

                val (release, raw) = respData
                if (release == null) {
                    releaseResult.value = FetchResult.LocalizedError(R.string.release_not_found)
                    return@launch
                }

                releaseResult.value = FetchResult.Success(release)

                withContext(Dispatchers.IO) {
                    cache.saveText(release.cacheFileName(), raw)
                }

            } catch (e: ServerError) {
                releaseResult.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.release_not_found)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                releaseResult.value = FetchResult.fromException(e)
            }
        }
    }
}
