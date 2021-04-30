package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.request.PasswordResetParams
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseApiError
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger

class PasswordResetViewModel : ViewModel(), AnkoLogger {
    val isNetworkAvailable = MutableLiveData<Boolean>()
    val progressLiveData = MutableLiveData<Boolean>()

    val passwordLiveData = MutableLiveData("")
    val passwordValidator = LiveDataValidator(passwordLiveData).apply {
        addRule("密码不能为空", Validator::notEmpty)
        addRule("长度不能少于8位", Validator.minLength(8))
    }

    val confirmPasswordLiveData = MutableLiveData("")
    val confirmPasswordValidator = LiveDataValidator(confirmPasswordLiveData).apply {
        addRule("确认密码不能为空", Validator::notEmpty)
        addRule("长度不能少于8位", Validator.minLength(8))
        addRule("两次输入的密码不同") {
            it != null && it == passwordLiveData.value
        }
    }

    private val isDirty: Boolean
        get() = !passwordLiveData.value.isNullOrBlank() && !confirmPasswordLiveData.value.isNullOrBlank()

    private val formValidator = LiveDataValidatorResolver(listOf(passwordValidator, confirmPasswordValidator))

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(passwordLiveData) {
            value = enableForm()
        }
        addSource(progressLiveData) {
            value = enableForm()
        }
    }

    private fun enableForm(): Boolean {
        return progressLiveData.value == false && isDirty && formValidator.isValid()
    }

    init {
        progressLiveData.value = false
    }

    val resetResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    fun resetPassword(token: String) {
        if (isNetworkAvailable.value != true) {
            resetResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        val params = PasswordResetParams(
            token = token,
            password = passwordLiveData.value ?: ""
        )

        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    AuthClient.resetPassword(params)
                }

                resetResult.value = Result.Success(ok)

            } catch (e: ClientError) {
                // TODO: handle 422 error
                if (e.statusCode == 404) {
                    resetResult.value = Result.LocalizedError(R.string.api_email_not_found)
                } else {
                    resetResult.value = parseApiError(e)
                }
            } catch (e: Exception) {
                resetResult.value = parseException(e)
            }
        }
    }
}
