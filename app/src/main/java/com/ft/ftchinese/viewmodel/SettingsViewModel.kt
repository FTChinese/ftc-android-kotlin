package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ReadingHistoryDao
import com.ft.ftchinese.model.AppRelease
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

    val latestReleaseResult = MutableLiveData<LatestReleaseResult>()

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

    fun checkLatestRelease() {
        viewModelScope.launch {
            try {
                val latest = withContext(Dispatchers.IO) {
                    fetchLatestRelease()
                }

                latestReleaseResult.value = LatestReleaseResult(
                        success = latest
                )

            } catch (e: ClientError) {
                latestReleaseResult.value = LatestReleaseResult(
                        error = if (e.statusCode == 404) {
                            R.string.not_found_latest_app
                        } else null,
                        exception = e
                )
            } catch (e: Exception) {
                latestReleaseResult.value = LatestReleaseResult(
                        exception = e
                )
            }
        }
    }

    private fun fetchLatestRelease(): AppRelease? {
        val (_, body) = Fetch()
                .setAppId()
                .get(NextApi.latestRelease)
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse(body)
        }
    }
}
