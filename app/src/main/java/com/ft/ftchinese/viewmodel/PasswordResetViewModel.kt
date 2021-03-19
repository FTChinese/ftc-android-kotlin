package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.PasswordResetter
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.model.reader.PwResetVerifier
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.repository.ReaderRepo
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger

class PasswordResetViewModel : ViewModel(), AnkoLogger {
    val isNetworkAvailable = MutableLiveData<Boolean>()

    val formState: MutableLiveData<FormState> by lazy {
        MutableLiveData<FormState>()
    }

    val letterResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    val verificationResult: MutableLiveData<Result<PwResetBearer>> by lazy {
        MutableLiveData<Result<PwResetBearer>>()
    }

    val resetResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    fun emailDataChanged(email: String) {
        formState.value = FormState(
            error = Validator.ensureEmail(email),
            field = ControlField.Email
        )
    }

    fun passwordDataChanged(password: String) {
        formState.value = FormState(
            error = Validator.ensurePassword(password),
            field = ControlField.Password
        )
    }

    fun confirmPwDataChanged(password: String) {
        formState.value = FormState(
            error = Validator.ensurePassword(password),
            field = ControlField.ConfirmPassword
        )
    }

    fun codeDataChanged(code: String) {
        formState.value = FormState(
            error = if (Validator.validateCode(code)) null else R.string.error_invalid_code,
            field = ControlField.ConfirmPassword
        )
    }

    fun requestResettingPassword(email: String) {
        if (isNetworkAvailable.value != true) {
            letterResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    ReaderRepo.passwordResetLetter(email)
                }

                letterResult.value = Result.Success(ok)

            } catch (e: ClientError) {
                if (e.statusCode == 404) {
                    letterResult.value = Result.LocalizedError(R.string.api_email_not_found)
                } else {
                    letterResult.value = parseApiError(e)
                }
            } catch (e: Exception) {
                letterResult.value = parseException(e)
            }
        }
    }

    fun verifyPwResetCode(v: PwResetVerifier) {
        if (isNetworkAvailable.value != true) {
            verificationResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val bearer = withContext(Dispatchers.IO) {
                    ReaderRepo.verifyPwResetCode(v)
                }

                if (bearer == null) {
                    verificationResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                verificationResult.value = Result.Success(bearer)

            } catch (e: ClientError) {
                if (e.statusCode == 404) {
                    verificationResult.value = Result.LocalizedError(R.string.api_password_code_not_found)
                } else {
                    verificationResult.value = parseApiError(e)
                }
            } catch (e: Exception) {
                verificationResult.value = parseException(e)
            }
        }
    }

    fun resetPassword(r: PasswordResetter) {
        if (isNetworkAvailable.value != true) {
            resetResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    ReaderRepo.resetPassword(r)
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
