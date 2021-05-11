package com.ft.ftchinese.ui.account.password

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.request.PasswordUpdateParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PasswordViewModel : BaseViewModel() {
    val oldPasswordLiveData = MutableLiveData("")
    val oldPasswordValidator = LiveDataValidator(oldPasswordLiveData).apply {
        addRule("必须输入当前密码", Validator::notEmpty)
    }

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
        get() = !oldPasswordLiveData.value.isNullOrBlank() && !passwordLiveData.value.isNullOrBlank() && !confirmPasswordLiveData.value.isNullOrBlank()

    private val formValidator = LiveDataValidatorResolver(listOf(
        oldPasswordValidator,
        passwordValidator,
        confirmPasswordValidator))

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

    val updated: MutableLiveData<FetchResult<Boolean>> by lazy {
        MutableLiveData<FetchResult<Boolean>>()
    }

    fun updatePassword() {
        if (isNetworkAvailable.value != true) {
            updated.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        val userId = AccountCache.get()?.id ?: return
        val params = PasswordUpdateParams(
            oldPassword = oldPasswordLiveData.value ?: "",
            password = passwordLiveData.value ?: ""
        )

        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.updatePassword(userId, params)
                }

                progressLiveData.value = false
                updated.value = FetchResult.Success(done)
                isFormEnabled.value = !done
            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    403 -> R.string.error_incorrect_old_password
                    404 -> R.string.api_account_not_found
                    422 -> when (e.error?.key) {
                        "password_invalid" -> R.string.signup_invalid_password
                        else -> null
                    }
                    else -> null
                }

                updated.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromServerError(e)
                }
                progressLiveData.value = false
            } catch (e: Exception) {
                updated.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }
}
