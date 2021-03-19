package com.ft.ftchinese.viewmodel

import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Passwords
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateViewModel : ViewModel() {
    val inProgress = MutableLiveData<Boolean>()

    val updateFormState: MutableLiveData<UpdateFormState> by lazy {
        MutableLiveData<UpdateFormState>()
    }

    val updateResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    val sendEmailResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    fun emailDataChanged(currentEmail: String, newEmail: String) {
        if (!isEmailValid(newEmail)) {
            updateFormState.value = UpdateFormState(
                    emailError = R.string.error_invalid_email
            )
            return
        }

        if (currentEmail == newEmail) {
            updateFormState.value = UpdateFormState(
                    emailError = R.string.error_email_unchanged
            )
            return
        }

        updateFormState.value = UpdateFormState(
                isDataValid = true
        )
    }

    private fun isEmailValid(email: String): Boolean {
        return if (!email.contains('@')) {
            false
        } else {
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }
    }

    fun updateEmail(userId: String, email: String) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.updateEmail(userId, email)
                }

                updateResult.value = Result.Success(done)
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 422) {
                    when (e.error?.key) {
                        "email_already_exists" -> R.string.api_email_taken
                        "email_invalid" -> R.string.error_invalid_email
                        else -> null
                    }
                } else {
                    null
                }

                updateResult.value = if (msgId != null) {
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                updateResult.value = parseException(e)
            }
        }
    }

    fun userNameDataChanged(currentName: String?, newName: String) {
        if (!isNameValid(newName)) {
            updateFormState.value = UpdateFormState(
                    nameError = R.string.error_invalid_name
            )
            return
        }

        if (currentName == newName) {
            updateFormState.value = UpdateFormState(
                    nameError = R.string.error_name_unchanged
            )
            return
        }

        updateFormState.value = UpdateFormState(
            isDataValid = true
        )
    }

    private fun isNameValid(name: String): Boolean {
        return name.isNotBlank() && name.length <= 32
    }

    fun updateUserName(userId: String, name: String) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.updateUserName(userId, name)
                }

                updateResult.value = Result.Success(done)

            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 422) {
                    when (e.error?.key) {
                        "userName_already_exists" -> R.string.api_name_taken
                        else -> null
                    }
                } else {
                    null
                }

                updateResult.value = if (msgId != null) {
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }
            } catch (e: Exception) {
                updateResult.value = parseException(e)
            }
        }
    }

    fun updatePassword(userId: String, passwords: Passwords) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.updatePassword(userId, passwords)
                }

                updateResult.value = Result.Success(done)

            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    403 -> R.string.error_incorrect_old_password
                    404 -> R.string.api_account_not_found
                    422 -> when (e.error?.key) {
                        "password_invalid" -> R.string.error_invalid_password
                        else -> null
                    }
                    else -> null
                }

                updateResult.value = if (msgId != null) {
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                updateResult.value = parseException(e)
            }

        }
    }

    fun addressDataChanged(current: Address, updated: Address) {
        if (current == updated) {
            return
        }

        updateFormState.value = UpdateFormState(
            isDataValid = true
        )
    }

    fun requestVerification(userId: String) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.requestVerification(userId)
                }

                sendEmailResult.value = Result.Success(done)

            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    404 -> R.string.api_account_not_found
                    422 -> when (e.error?.key) {
                        "email_server_missing" -> R.string.api_email_server_down
                        else -> null
                    }
                    else -> null
                }

                sendEmailResult.value = if (msgId != null) {
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                sendEmailResult.value = parseException(e)
            }
        }
    }

    fun showProgress(show: Boolean) {
        inProgress.value = show
    }
}
