package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.model.request.PasswordResetLetterParams
import com.ft.ftchinese.model.request.PasswordResetVerifier
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseApiError
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger

class ForgotPasswordViewModel : ViewModel(), AnkoLogger {
    val isNetworkAvailable = MutableLiveData<Boolean>()
    val progressLiveData = MutableLiveData<Boolean>()

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

    val letterSent: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    val verificationResult: MutableLiveData<Result<PwResetBearer>> by lazy {
        MutableLiveData<Result<PwResetBearer>>()
    }

    // Ask for a email containing code to verify this password reset session.
    fun requestCode() {
        if (isNetworkAvailable.value != true) {
            letterSent.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val params = PasswordResetLetterParams(
            email = emailLiveData.value ?: ""
        )

        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
//                    AuthClient.passwordResetLetter(params)
                    delay(3000)
                    true
                }

                letterSent.value = Result.Success(ok)
                progressLiveData.value = false
            } catch (e: ClientError) {
                progressLiveData.value = false
                if (e.statusCode == 404) {
                    letterSent.value = Result.LocalizedError(R.string.api_email_not_found)
                } else {
                    letterSent.value = parseApiError(e)
                }
            } catch (e: Exception) {
                letterSent.value = parseException(e)
                progressLiveData.value = false
            }
        }
    }

    fun verifyCode() {
        if (isNetworkAvailable.value != true) {
            verificationResult.value = Result.LocalizedError(R.string.prompt_no_network)
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
//                    AuthClient.verifyPwResetCode(params)
                    delay(2000)
                    PwResetBearer(
                        email = params.email,
                        token = "abcedfghijklmnopqrstuvwxyz"
                    )
                }

                progressLiveData.value = false

                if (bearer == null) {
                    verificationResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                verificationResult.value = Result.Success(bearer)
            } catch (e: ClientError) {
                progressLiveData.value = false
                if (e.statusCode == 404) {
                    verificationResult.value =
                        Result.LocalizedError(R.string.api_password_code_not_found)
                } else {
                    verificationResult.value = parseApiError(e)
                }
            } catch (e: Exception) {
                verificationResult.value = parseException(e)
                progressLiveData.value = false
            }
        }
    }
}
