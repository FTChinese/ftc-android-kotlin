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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val cache = FileCache(application)
    private val readingHistoryDao = ArticleDb.getInstance(application).readDao()

    val progressLiveData: MutableLiveData<Boolean> by lazy {
        MutableLiveData(false)
    }

    val fcmStatusLiveData: MutableLiveData<List<IconTextRow>> by lazy {
        MutableLiveData(listOf())
    }

    val cacheSizeLiveData = MutableLiveData<String>()
    val articlesReadLiveData = MutableLiveData<Int>()

    val toastMessage: MutableLiveData<ToastMessage> by lazy {
        MutableLiveData<ToastMessage>()
    }

    fun clearToast() {
        toastMessage.value = null
    }

    fun calculateCacheSize() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cache.space()
            }.let {
                cacheSizeLiveData.value = it
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
                        withContext(Dispatchers.IO) {
                            cache.space()
                        }.let { size ->
                            cacheSizeLiveData.value = size
                        }
                        R.string.prompt_cache_cleared
                    } else {
                        R.string.prompt_cache_not_cleared
                    }
                )
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

    fun checkFcm() {
        val msgBuilder = FcmMessageBuilder()
        progressLiveData.value = true

        val playAvailable = checkPlayServices()
        fcmStatusLiveData.value = msgBuilder
            .addPlayService(playAvailable)
            .build()

        retrieveRegistrationToken {
            fcmStatusLiveData.value = msgBuilder
                .addTokenRetrievable(it.isSuccessful)
                .build()

            progressLiveData.value = false
        }
    }

    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(getApplication())

        return resultCode == ConnectionResult.SUCCESS
    }

    private fun retrieveRegistrationToken(listener: OnCompleteListener<String>) {
        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener(listener)
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
