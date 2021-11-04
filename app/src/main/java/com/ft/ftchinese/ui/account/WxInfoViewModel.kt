package com.ft.ftchinese.ui.account

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.ApiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream

private const val TAG = "WxInfoViewModel"

class WxInfoViewModel : BaseViewModel() {

    val accountLoaded: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    val avatarLoaded: MutableLiveData<FetchResult<InputStream>> by lazy {
        MutableLiveData<FetchResult<InputStream>>()
    }

    // Should we show reauth dialog?
    val sessionState: MutableLiveData<WxRefreshState> by lazy {
        MutableLiveData<WxRefreshState>()
    }

    // Load avatar, upon initial start, from cache, and fallback to network.
    fun loadAvatar(wechat: Wechat, cache: FileCache) {
        wechat.avatarUrl ?: return

        Log.i(TAG, "Start loading wechat avatar: ${wechat.avatarUrl}")


        viewModelScope.launch {
            try {
                val fis = withContext(Dispatchers.IO) {
                    cache.readBinaryFile(CacheFileNames.wxAvatar)
                }

                if (fis != null) {
                    Log.i(TAG, "Wx avatar loaded from cache")
                    avatarLoaded.value = FetchResult.Success(fis)
                    return@launch
                }

                if (isNetworkAvailable.value == false) {
                    avatarLoaded.value = FetchResult.LocalizedError(R.string.prompt_no_network)
                    return@launch
                }

                val bytes = withContext(Dispatchers.IO) {
                    AccountRepo.loadWxAvatar(wechat.avatarUrl)
                } ?: return@launch

                Log.i(TAG, "Avatar loaded from server")
                avatarLoaded.value = FetchResult.Success(ByteArrayInputStream(bytes))

                withContext(Dispatchers.IO) {
                    cache.writeBinaryFile(
                        CacheFileNames.wxAvatar,
                        bytes
                    )
                }
            } catch (e: Exception) {
                avatarLoaded.value = FetchResult.fromException(e)
            }
        }
    }

    // Refresh wechat info using current session.
    // The session might be expired.
    // Retrieve account only after session refreshed.
    fun refresh(account: Account, sess: WxSession) {
        if (isNetworkAvailable.value == false) {
            accountLoaded.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.refreshWxInfo(wxSession = sess)
                }

                if (done) {
                    sessionState.value = WxRefreshState.SUCCESS
                    accountLoaded.value = ApiRequest.asyncRefreshAccount(account)
                } else {
                    sessionState.value = WxRefreshState.ReAuth
                }

                progressLiveData.value = false
            } catch (e: APIError) {
                progressLiveData.value = false
                Log.i(TAG, "$e")
                accountLoaded.value = when (e.statusCode) {
                    404 -> FetchResult.LocalizedError(R.string.account_not_found)
                    // TODO: handle 422?
                    else -> FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                Log.i(TAG, "$e")
                progressLiveData.value = false
                accountLoaded.value = FetchResult.fromException(e)
            }
        }
    }

    // Retrieve avatar from network.
    fun refreshAvatar(wechat: Wechat, cache: FileCache) {

        if (isNetworkAvailable.value == false) {
            avatarLoaded.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        wechat.avatarUrl ?: return

        progressLiveData.value = true
        // No progress indicator for avatar
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    AccountRepo.loadWxAvatar(wechat.avatarUrl)
                } ?: return@launch

                progressLiveData.value = false
                avatarLoaded.value = FetchResult.Success(ByteArrayInputStream(bytes))

                withContext(Dispatchers.IO) {
                    cache.writeBinaryFile(
                        CacheFileNames.wxAvatar,
                        bytes
                    )
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                avatarLoaded.value = FetchResult.fromException(e)
            }
        }
    }
}
