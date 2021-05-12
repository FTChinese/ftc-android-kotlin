package com.ft.ftchinese.wxapi

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ServerError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.model.request.WxAuthParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.model.fetch.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class WxOAuthViewModel : BaseViewModel(), AnkoLogger {

    // Wechat OAuth result.
    val wxSessionResult: MutableLiveData<FetchResult<WxSession>> by lazy {
        MutableLiveData<FetchResult<WxSession>>()
    }

    val accountResult: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    init {
        progressLiveData.value = true
    }

    /**
     * Uses wechat authorization code to get an access token, and then use the
     * token to get user info.
     * API responds with WxSession data to uniquely identify this login
     * session.
     * You can use the session data later to retrieve user account.
     */
    fun getSession(code: String) {
        if (isNetworkAvailable.value == false) {
            wxSessionResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val params = WxAuthParams(
            code = code,
        )
        viewModelScope.launch {
            try {
                info("Start requesting wechat oauth session data")
                val sess = withContext(Dispatchers.IO) {
                    AuthClient.wxLogin(params)
                }

                // Fetched wx session data and send it to
                // UI thread for saving, and then continues
                // to fetch account data.
                if (sess == null) {
                    progressLiveData.value = false
                    info("Wechat oauth session is null")
                    wxSessionResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                wxSessionResult.value = FetchResult.Success(sess)
                // After session data fetched, continue to fetch account data.
                loadAccount(sess)
            } catch (e: Exception) {
                // If the error is ClientError,
                // Possible 422 error key: code_missing_field, code_invalid.
                // We cannot make sure the exact meaning of each error, just
                // show user API's error message.

                info(e)
                progressLiveData.value = false
                wxSessionResult.value = FetchResult.fromException(e)
            }
        }
    }

    /**
     * Load account after user performed wechat authorization
     */
    suspend fun loadAccount(wxSession: WxSession) {

        if (isNetworkAvailable.value == false) {
            accountResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        try {
            val account = withContext(Dispatchers.IO) {
                AccountRepo.loadWxAccount(wxSession.unionId)
            }

            progressLiveData.value = false

            if (account == null) {
                accountResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                return
            }

            info("Loaded wechat account: $account")

            accountResult.value = FetchResult.Success(account)
        } catch (e: ServerError) {
            info("Retrieving wechat account error $e")
            progressLiveData.value = false
            accountResult.value = if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.fromServerError(e)
            }

        } catch (e: Exception) {
            progressLiveData.value = false
            info(e)
            accountResult.value = FetchResult.fromException(e)
        }
    }
}
