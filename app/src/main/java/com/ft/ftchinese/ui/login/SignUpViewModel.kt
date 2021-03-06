package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ServerError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.model.request.MobileLinkParams
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.model.fetch.FetchResult
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

    val mobileLiveData = MutableLiveData("")

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
    fun emailSignUp(deviceToken: String) {

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
            } catch (e: ServerError) {
                progressLiveData.value = false
                handleSignUpError(e)
            } catch (e: Exception) {
                progressLiveData.value = false
                accountResult.value = FetchResult.fromException(e)
            }
        }
    }

    fun mobileSignUp(deviceToken: String) {
        if (isNetworkAvailable.value == false) {
            accountResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val params = MobileLinkParams(
            email = emailLiveData.value ?: "",
            password = passwordLiveData.value ?: "",
            mobile = mobileLiveData.value ?: "",
            deviceToken = deviceToken,
        )

        viewModelScope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    AuthClient.mobileSignUp(params)
                }

                progressLiveData.value = false

                if (account == null) {
                    accountResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                accountResult.value = FetchResult.Success(account)
            } catch (e: ServerError) {
                progressLiveData.value = false
                handleSignUpError(e)
            } catch (e: Exception) {
                progressLiveData.value = false
                accountResult.value = FetchResult.fromException(e)
            }
        }
    }

    private fun handleSignUpError(e: ServerError) {
        accountResult.value = when (e.statusCode) {
            422 -> {
                if (e.error == null) {
                    FetchResult.fromServerError(e)
                } else {
                    when {
                        e.error.isFieldAlreadyExists("email") -> FetchResult.LocalizedError(R.string.signup_email_taken)
                        e.error.isFieldInvalid("email") -> FetchResult.LocalizedError(R.string.signup_invalid_email)
                        e.error.isFieldInvalid("password") -> FetchResult.LocalizedError(R.string.signup_invalid_password)
                        else -> FetchResult.fromServerError(e)
                    }
                }
            }
            else -> FetchResult.fromServerError(e)
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

                progressLiveData.value = false
                if (account == null) {
                    accountResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                accountResult.value = FetchResult.Success(account)
            } catch (e: ServerError) {
                info(e)
                progressLiveData.value = false
                handleSignUpError(e)
            } catch (e: Exception) {
                info(e)
                accountResult.value = FetchResult.fromException(e)
            }
        }
    }
}
