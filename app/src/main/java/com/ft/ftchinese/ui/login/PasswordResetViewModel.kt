package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.request.PasswordResetParams
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.launch

class PasswordResetViewModel : BaseViewModel() {
    val passwordLiveData = MutableLiveData("")
    val passwordValidator = LiveDataValidator(passwordLiveData).apply {
        addRule("密码不能为空", Validator::notEmpty)
        addRule("不能包含空格", Validator::containNoSpace)
        addRule("长度不能少于8位", Validator.minLength(8))
    }

    val confirmPasswordLiveData = MutableLiveData("")
    val confirmPasswordValidator = LiveDataValidator(confirmPasswordLiveData).apply {
        addRule("确认密码不能为空", Validator::notEmpty)
        addRule("不能包含空格", Validator::containNoSpace)
        addRule("长度不能少于8位", Validator.minLength(8))
        addRule("两次输入的密码不同") {
            it != null && it == passwordLiveData.value
        }
    }

    private val formValidator = LiveDataValidatorResolver(listOf(passwordValidator, confirmPasswordValidator))

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(passwordLiveData) {
            value = enableForm()
        }
        addSource(confirmPasswordLiveData) {
            value = enableForm()
        }
        addSource(progressLiveData) {
            value = enableForm()
        }
    }

    private fun enableForm(): Boolean {
        // If in progress, disable form.
        if (progressLiveData.value == true) {
            return false
        }

        // Not in progress.
        if (passwordLiveData.value?.trim().isNullOrBlank()) {
            return false
        }

        if (passwordLiveData.value?.trim().isNullOrBlank()) {
            return false
        }

        return formValidator.isValid()
    }

    init {
        progressLiveData.value = false
    }

    val resetResult: MutableLiveData<FetchResult<Boolean>> by lazy {
        MutableLiveData<FetchResult<Boolean>>()
    }

    fun resetPassword(token: String) {
        if (isNetworkAvailable.value != true) {
            resetResult.value = FetchResult.notConnected
            return
        }

        progressLiveData.value = true
        val params = PasswordResetParams(
            token = token,
            password = passwordLiveData.value?.trim() ?: ""
        )

        viewModelScope.launch {
            val result = AuthClient.asyncResetPassword(params)
            progressLiveData.value = false
            resetResult.value = result
        }
    }
}
