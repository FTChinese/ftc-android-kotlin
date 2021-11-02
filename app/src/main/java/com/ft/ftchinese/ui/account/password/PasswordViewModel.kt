package com.ft.ftchinese.ui.account.password

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.request.PasswordUpdateParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.model.fetch.FetchResult
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
        addRule("新密码不能为空", Validator::notEmpty)
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
        get() = oldPasswordValidator.isDirty() || passwordValidator.isDirty() || confirmPasswordValidator.isDirty()

    private val formValidator = LiveDataValidatorResolver(listOf(
        oldPasswordValidator,
        passwordValidator,
        confirmPasswordValidator))

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(oldPasswordLiveData) {
            value = enableForm()
        }
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
            currentPassword = oldPasswordLiveData.value?.trim() ?: "",
            newPassword = passwordLiveData.value?.trim() ?: ""
        )

        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.updatePassword(userId, params)
                }

                progressLiveData.value = false
                updated.value = FetchResult.Success(done)
                clear()
            } catch (e: APIError) {
                progressLiveData.value = false

                updated.value = when (e.statusCode) {
                    403 -> FetchResult.LocalizedError(R.string.password_current_incorrect)
                    404 -> FetchResult.LocalizedError(R.string.account_not_found)
                    422 -> when {
                        e.error == null -> FetchResult.fromServerError(e)
                        e.error.isFieldInvalid("password") -> FetchResult.LocalizedError(R.string.signup_invalid_password)
                        else -> FetchResult.fromServerError(e)
                    }
                    else -> FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                progressLiveData.value = false
                updated.value = FetchResult.fromException(e)
            }
        }
    }

    private fun clear() {
        oldPasswordLiveData.value = ""
        passwordLiveData.value = ""
        confirmPasswordLiveData.value = ""
    }
}
