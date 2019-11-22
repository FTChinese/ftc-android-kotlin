package com.ft.ftchinese.ui.account

import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Passwords
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.util.ClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateViewModel : ViewModel() {
    val inProgress = MutableLiveData<Boolean>()

    val updateFormState: MutableLiveData<UpdateFormState> by lazy {
        MutableLiveData<UpdateFormState>()
    }

    private val _updateResult = MutableLiveData<BinaryResult>()
    val updateResult: MutableLiveData<BinaryResult> by lazy {
        MutableLiveData<BinaryResult>()
    }

    val sendEmailResult = MutableLiveData<BinaryResult>()

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
                    AccountRepo().updateEmail(userId, email)
                }

                _updateResult.value = BinaryResult(
                        success = done
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 422) {
                    when (e.error?.key) {
                        "email_already_exists" -> R.string.api_email_taken
                        "email_invalid" -> R.string.error_invalid_email
                        else -> null
                    }
                } else {
                    e.parseStatusCode()
                }

                _updateResult.value = BinaryResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                _updateResult.value = BinaryResult(
                        exception = e
                )
            }
        }
    }

    fun userNameDataChanged(currentName: String, newName: String) {
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
                    AccountRepo().updateUserName(userId, name)
                }

                updateResult.value = BinaryResult(
                        success = done
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 422) {
                    when (e.error?.key) {
                        "userName_already_exists" -> R.string.api_name_taken
                        else -> null
                    }
                } else {
                    e.parseStatusCode()
                }

                updateResult.value = BinaryResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                updateResult.value = BinaryResult(
                        exception = e
                )
            }
        }
    }

    fun updatePassword(userId: String, passwords: Passwords) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo().updatePassword(userId, passwords)
                }

                updateResult.value = BinaryResult(
                        success = done
                )
            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    403 -> R.string.error_incorrect_old_password
                    404 -> R.string.api_account_not_found
                    422 -> when (e.error?.key) {
                        "password_invalid" -> R.string.error_invalid_password
                        else -> null
                    }
                    else -> e.parseStatusCode()
                }

                updateResult.value = BinaryResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                updateResult.value = BinaryResult(
                        exception = e
                )
            }

        }
    }

    fun requestVerification(userId: String) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo().requestVerification(userId)
                }

                sendEmailResult.value = BinaryResult(
                        success = done
                )

            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    404 -> R.string.api_account_not_found
                    422 -> when (e.error?.key) {
                        "email_server_missing" -> R.string.api_email_server_down
                        else -> null
                    }
                    else -> e.parseStatusCode()
                }

                sendEmailResult.value = BinaryResult(
                        error = msgId,
                        exception = e
                )

            } catch (e: Exception) {
                sendEmailResult.value = BinaryResult(
                        exception = e
                )
            }
        }
    }

    fun showProgress(show: Boolean) {
        inProgress.value = show
    }
}
