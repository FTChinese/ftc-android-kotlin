package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.FetchResult
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

    fun login(deviceToken: String) {
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
            } catch (e: ClientError) {
                progressLiveData.value = false
                accountResult.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.error_invalid_password)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                progressLiveData.value = false
                accountResult.value = FetchResult.fromException(e)
            }
        }
    }
}
