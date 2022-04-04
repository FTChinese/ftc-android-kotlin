package com.ft.ftchinese.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.components.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val cache = FileCache(application)
    private val readingHistoryDao = ArticleDb.getInstance(application).readDao()

    val cacheSizeLiveData = MutableLiveData<String>()
    val articlesReadLiveData = MutableLiveData<Int>()

    val toastMessage: MutableLiveData<ToastMessage> by lazy {
        MutableLiveData<ToastMessage>()
    }

    fun calculateCacheSize() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cache.space()
            }.let {
                cacheSizeLiveData.value
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cache.clear()
            }.let {
                toastMessage.value = ToastMessage.Resource(
                    if (it) {
                        R.string.prompt_cache_cleared
                    } else {
                        R.string.prompt_cache_not_cleared
                    }
                )
            }

            withContext(Dispatchers.IO) {
                cache.space()
            }.let {
                cacheSizeLiveData.value
            }
        }
    }

    fun countReadArticles() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                readingHistoryDao.count()
            }.let {
                articlesReadLiveData.value = it
            }
        }
    }

    fun truncateReadArticles() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                readingHistoryDao.deleteAll()
                readingHistoryDao.vacuumDb(SimpleSQLiteQuery("VACUUM"))
            }

            Log.i(TAG, "Truncated $result")
            articlesReadLiveData.value = 0

            toastMessage.value = ToastMessage.Resource(R.string.prompt_reading_history)
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
