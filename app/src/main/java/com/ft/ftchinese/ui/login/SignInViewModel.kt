package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.model.request.MobileLinkParams
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.launch

class SignInViewModel : BaseViewModel() {

    val emailLiveData = MutableLiveData("")

    val passwordLiveData = MutableLiveData("")
    val passwordValidator = LiveDataValidator(passwordLiveData).apply {
        addRule("密码不能为空", Validator::notEmpty)
    }

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enableSubmit()
        }
        addSource(passwordLiveData) {
            value = enableSubmit()
        }
    }

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
            val result = AuthClient.asyncEmailLogin(credentials)
            progressLiveData.value = false
            accountResult.value = result
        }
    }

    // A mobile number is used for the first time for login.
    fun mobileLinkEmail(mobile: String, deviceToken: String) {
        if (isNetworkAvailable.value == false) {
            accountResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val params = MobileLinkParams(
            email = emailLiveData.value ?: "",
            password = passwordLiveData.value ?: "",
            mobile = mobile,
            deviceToken = deviceToken
        )

        viewModelScope.launch {
            val result = AuthClient.asyncMobileLinkEmail(params)
            progressLiveData.value = false
            accountResult.value = result
        }
    }
}
