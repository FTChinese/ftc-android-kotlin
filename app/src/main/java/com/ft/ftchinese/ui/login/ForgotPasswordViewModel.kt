package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.model.request.PasswordResetLetterParams
import com.ft.ftchinese.model.request.PasswordResetVerifier
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger

class ForgotPasswordViewModel : BaseViewModel(), AnkoLogger {
    val emailLiveData = MutableLiveData("")
    val emailValidator = LiveDataValidator(emailLiveData).apply {
        addRule("请输入正确的邮箱", Validator::isEmail)
    }

    val codeLiveData = MutableLiveData("")
    val codeValidator = LiveDataValidator(codeLiveData).apply {
        addRule("请输入验证码", Validator.minLength(6))
    }

    val counterLiveData = MutableLiveData<Int>()

    private val isDirty: Boolean
        get() = !emailLiveData.value.isNullOrBlank() && !codeLiveData.value.isNullOrBlank()

    private val formValidator = LiveDataValidatorResolver(listOf(emailValidator, codeValidator))

    val isCodeRequestEnabled = MediatorLiveData<Boolean>().apply {
        addSource(emailLiveData) {
            value = enableCodeRequest()
        }
        addSource(progressLiveData) {
            value = enableCodeRequest()
        }
        addSource(counterLiveData) {
            value = enableCodeRequest()
        }
    }

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(emailLiveData) {
            value = enableForm()
        }
        addSource(codeLiveData) {
            value = enableForm()
        }
        addSource(progressLiveData) {
            value = enableForm()
        }
    }

    private fun enableCodeRequest(): Boolean {
        if (counterLiveData.value != 0) {
            return false
        }

        return progressLiveData.value == false && !emailLiveData.value.isNullOrBlank() && emailValidator.isValid()
    }

    private fun enableForm(): Boolean {
        return progressLiveData.value == false && isDirty && formValidator.isValid()
    }

    init {
        counterLiveData.value = 0
        progressLiveData.value = false
        isFormEnabled.value = false
    }

    val letterSent: MutableLiveData<FetchResult<Boolean>> by lazy {
        MutableLiveData<FetchResult<Boolean>>()
    }

    val verificationResult: MutableLiveData<FetchResult<PwResetBearer>> by lazy {
        MutableLiveData<FetchResult<PwResetBearer>>()
    }

    // Ask for a email containing code to verify this password reset session.
    fun requestCode() {
        if (isNetworkAvailable.value != true) {
            letterSent.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val params = PasswordResetLetterParams(
            email = emailLiveData.value ?: ""
        )

        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    AuthClient.passwordResetLetter(params)
                }

                letterSent.value = FetchResult.Success(ok)
                progressLiveData.value = false
            } catch (e: ClientError) {
                progressLiveData.value = false
                letterSent.value = when (e.statusCode) {
                    404 -> FetchResult.LocalizedError(R.string.login_email_not_found)
                    else -> FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                letterSent.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }

    fun verifyCode() {
        if (isNetworkAvailable.value != true) {
            verificationResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val params = PasswordResetVerifier(
            email = emailLiveData.value ?: "",
            code = codeLiveData.value ?: ""
        )

        viewModelScope.launch {
            try {
                val bearer = withContext(Dispatchers.IO) {
                    AuthClient.verifyPwResetCode(params)
                }

                progressLiveData.value = false

                if (bearer == null) {
                    verificationResult.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                verificationResult.value = FetchResult.Success(bearer)
            } catch (e: ClientError) {
                progressLiveData.value = false
                verificationResult.value = when (e.statusCode) {
                    404 -> FetchResult.LocalizedError(R.string.forgot_password_code_not_found)
                    else -> FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                verificationResult.value = FetchResult.fromException(e)
            }
        }
    }
}
