package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class SignUpViewModel : ViewModel(), AnkoLogger {
    val progressLiveData = MutableLiveData<Boolean>()
    val isNetworkAvailable = MutableLiveData<Boolean>()

    val emailLiveData = MutableLiveData("")

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
            value = enableSubmit()
        }
        addSource(progressLiveData) {
            value = enableSubmit()
        }
        addSource(confirmPasswordLiveData) {
            value = enableSubmit()
        }
    }

    private fun enableSubmit(): Boolean {
        return progressLiveData.value == false && isDirty && formValidator.isValid()
    }

    init {
        progressLiveData.value = false
    }

    val accountResult: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    /**
     * Handles both a new user signup, or wechat-logged-in
     * user trying to link to a new account.
     */
    fun signUp(deviceToken: String) {

        if (isNetworkAvailable.value == false) {
            accountResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val c = Credentials(
            email = emailLiveData.value ?: "",
            password = passwordLiveData.value ?: "",
            deviceToken = deviceToken
        )

        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    AuthClient.signUp(c)
                }

                progressLiveData.value = false
                if (account == null) {

                    accountResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                progressLiveData.value = false
                accountResult.value = FetchResult.Success(account)
            } catch (e: ClientError) {
                progressLiveData.value = false
                val msgId = if (e.statusCode == 422) {
                    when (e.error?.key) {
                        "email_already_exists" -> R.string.api_email_taken
                        "email_invalid" -> R.string.error_invalid_email
                        "password_invalid" -> R.string.error_invalid_password
                        else -> null
                    }
                } else {
                    null
                }

                accountResult.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                progressLiveData.value = false
                accountResult.value = FetchResult.fromException(e)
            }
        }
    }

    fun wxSignUp(deviceToken: String, unionId: String) {
        if (isNetworkAvailable.value == false) {
            accountResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val c = Credentials(
            email = emailLiveData.value ?: "",
            password = passwordLiveData.value ?: "",
            deviceToken = deviceToken
        )

        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    LinkRepo.signUp(c, unionId)
                }

                if (account == null) {

                    accountResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                accountResult.value = FetchResult.Success(account)
            } catch (e: ClientError) {
                info(e)
                val msgId = if (e.statusCode == 422) {
                    when (e.error?.key) {
                        "email_already_exists" -> R.string.api_email_taken
                        "email_invalid" -> R.string.error_invalid_email
                        "password_invalid" -> R.string.error_invalid_password
                        // handles wechat user sign up.
                        "account_link_already_taken" -> R.string.api_wechat_already_linked
                        "membership_link_already_taken" -> R.string.api_wechat_member_already_linked
                        else -> null
                    }
                } else {
                    null
                }

                accountResult.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                info(e)
                accountResult.value = FetchResult.fromException(e)
            }
        }
    }
}
