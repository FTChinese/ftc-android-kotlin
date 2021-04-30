package com.ft.ftchinese.wxapi

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseApiError
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class WxOAuthViewModel : ViewModel(), AnkoLogger {
    val progressLiveData = MutableLiveData<Boolean>()
    val isNetworkAvailable = MutableLiveData<Boolean>()

    // Wechat OAuth result.
    val wxSessionResult: MutableLiveData<Result<WxSession>> by lazy {
        MutableLiveData<Result<WxSession>>()
    }

    val accountResult: MutableLiveData<Result<Account>> by lazy {
        MutableLiveData<Result<Account>>()
    }

    /**
     * Uses wechat authrozation code to get an access token, and then use the
     * token to get user info.
     * API responds with WxSession data to uniquely identify this login
     * session.
     * You can use the session data later to retrieve user account.
     */
    fun getSession(code: String) {
        if (isNetworkAvailable.value == false) {
            wxSessionResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        viewModelScope.launch {
            try {
                info("Start requesting wechat oauth session data")
                val sess = withContext(Dispatchers.IO) {
                    AuthClient.wxLogin(code)
                }

                progressLiveData.value = false
                // Fetched wx session data and send it to
                // UI thread for saving, and then continues
                // to fetch account data.
                if (sess == null) {
                    info("Wechat oauth session is null")
                    wxSessionResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                wxSessionResult.value = Result.Success(sess)
            } catch (e: Exception) {
                // If the error is ClientError,
                // Possible 422 error key: code_missing_field, code_invalid.
                // We cannot make sure the exact meaning of each error, just
                // show user API's error message.

                info(e)
                progressLiveData.value = false
                wxSessionResult.value = parseException(e)
            }
        }
    }

    /**
     * Load account after user performed wechat authorization
     */
    fun loadAccount(wxSession: WxSession) {
        info("Start retrieving wechat account")

        if (isNetworkAvailable.value == false) {
            wxSessionResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    AccountRepo.loadWxAccount(wxSession.unionId)
                }

                progressLiveData.value = true

                if (account == null) {
                    accountResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                info("Loaded wechat account: $account")

                accountResult.value = Result.Success(account)
            } catch (e: ClientError) {
                info("Retrieving wechat account error $e")
                progressLiveData.value = false
                accountResult.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.loading_failed)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                progressLiveData.value = false
                info(e)
                accountResult.value = parseException(e)
            }
        }
    }
}
