package com.ft.ftchinese.ui.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream

private const val TAG = "WxInfoViewModel"

@Deprecated("")
class WxInfoViewModel : BaseViewModel() {

    val avatarLoaded: MutableLiveData<FetchResult<InputStream>> by lazy {
        MutableLiveData<FetchResult<InputStream>>()
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
}
