package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ServerError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.model.request.MobileLinkParams
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger

class SignInViewModel : BaseViewModel(), AnkoLogger {

    val emailLiveData = MutableLiveData("")

    val passwordLiveData = MutableLiveData("")
    val passwordValidator = LiveDataValidator(passwordLiveData).apply {
        addRule("密码不能为空", Validator::notEmpty)
        addRule("长度不能少于8位", Validator.minLength(8))
    }

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enableSubmit()
        }
        addSource(passwordLiveData) {
            value = enableSubmit()
        }
    }

    // Passed from MobileViewModel when the sign in ui
    // is launched by mobile login.
    val mobileLiveData = MutableLiveData("")

    init {
        progressLiveData.value = false
    }

    private fun enableSubmit(): Boolean {
        if (progressLiveData.value == true) {
            return false
        }

        if (passwordLiveData.value.isNullOrBlank()) {
            return false
        }

        return passwordValidator.isValid()
    }

    val accountResult: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    // Authenticate using email + password.
    // This could be a plain login with email, or authentication launched
    // by a wechat-only use to perform linking to this email account.
    fun emailAuth(deviceToken: String) {
        if (isNetworkAvailable.value == false) {
            accountResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val credentials = Credentials(
            email = emailLiveData.value ?: "",
            password = passwordLiveData.value ?: "",
            deviceToken = deviceToken,
        )

        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    AuthClient.login(credentials)
                }

                progressLiveData.value = false

                if (account == null) {

                    accountResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                accountResult.value = FetchResult.Success(account)
            } catch (e: ServerError) {
                progressLiveData.value = false
                handleLoginError(e)
            } catch (e: Exception) {
                progressLiveData.value = false
                accountResult.value = FetchResult.fromException(e)
            }
        }
    }

    // A mobile number is used for the first time for login.
    fun mobileLinkEmail(deviceToken: String) {
        if (isNetworkAvailable.value == false) {
            accountResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val params = MobileLinkParams(
            email = emailLiveData.value ?: "",
            password = passwordLiveData.value ?: "",
            mobile = mobileLiveData.value ?: "",
            deviceToken = deviceToken
        )

        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    AuthClient.mobileLinkEmail(params)
                }

                progressLiveData.value = false

                if (account == null) {

                    accountResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                accountResult.value = FetchResult.Success(account)
            } catch (e: ServerError) {
                progressLiveData.value = false

                handleLoginError(e)
            } catch (e: Exception) {
                progressLiveData.value = false
                accountResult.value = FetchResult.fromException(e)
            }
        }
    }

    private fun handleLoginError(e: ServerError) {
        if (e.error?.isFieldAlreadyExists("mobile") == true) {
            accountResult.value = FetchResult.LocalizedError(R.string.mobile_link_taken)
            return
        }

        accountResult.value = when (e.statusCode) {
            403 -> FetchResult.LocalizedError(R.string.login_incorrect_password)
            404 -> FetchResult.LocalizedError(R.string.account_not_found)
            else -> FetchResult.fromServerError(e)
        }
    }
}
