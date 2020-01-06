package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ReadingHistoryDao
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.repository.ClientError
import com.ft.ftchinese.repository.ReleaseRepo
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class SettingsViewModel : ViewModel(), AnkoLogger {

    val cacheSizeResult = MutableLiveData<String>()
    val cacheClearedResult = MutableLiveData<Boolean>()

    val articlesReadResult = MutableLiveData<Int>()
    val articlesClearedResult = MutableLiveData<Boolean>()

    val cachedReleaseFound = MutableLiveData<Boolean>()
    val releaseResult: MutableLiveData<Result<AppRelease>> by lazy {
        MutableLiveData<Result<AppRelease>>()
    }

    fun calculateCacheSize(cache: FileCache) {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) {
                cache.space()
            }

            cacheSizeResult.value = size
        }
    }

    fun clearCache(cache: FileCache) {
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

    fun loadCachedRelease(cache: FileCache) {
        viewModelScope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    val text = cache.loadText(AppRelease.currentCacheFile())

                    if (text == null) {
                        null
                    } else {
                        json.parse<AppRelease>(text)
                    }
                }

                if (release != null) {
                    cachedReleaseFound.value = true

                    releaseResult.value = Result.Success(release)

                    return@launch
                }

                cachedReleaseFound.value = false
            } catch (e: Exception) {
                cachedReleaseFound.value = false
            }
        }
    }

    fun fetchRelease(cache: FileCache, versionName: String) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    ReleaseRepo.retrieveRelease(versionName)
                }

                if (text == null) {
                    releaseResult.value = Result.LocalizedError(R.string.release_not_found)

                    return@launch
                }


                val appRelease = json.parse<AppRelease>(text)
                releaseResult.value = if (appRelease != null) {
                    Result.Success(appRelease)
                } else {
                    Result.LocalizedError(R.string.loading_failed)
                }

                withContext(Dispatchers.IO) {
                    cache.saveText(AppRelease.currentCacheFile(), text)
                }

            } catch (e: ClientError) {
                releaseResult.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.release_not_found)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                releaseResult.value = parseException(e)
            }
        }
    }

    fun checkLatestRelease() {
        viewModelScope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    ReleaseRepo.latestRelease()
                }

                if (release == null) {
                    releaseResult.value = Result.LocalizedError(R.string.release_not_found)
                    return@launch
                }

                releaseResult.value = Result.Success(release)

            } catch (e: Exception) {
                releaseResult.value = parseException(e)
            }
        }
    }
}
