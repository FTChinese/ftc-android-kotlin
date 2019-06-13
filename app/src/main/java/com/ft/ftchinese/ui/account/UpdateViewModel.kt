package com.ft.ftchinese.ui.account

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.FtcUser
import com.ft.ftchinese.model.Passwords
import com.ft.ftchinese.util.ClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateViewModel : ViewModel() {
    val inProgress = MutableLiveData<Boolean>()
    val done = MutableLiveData<Boolean>()

    private val _updateForm = MutableLiveData<UpdateFormState>()
    val updateFormState: LiveData<UpdateFormState> = _updateForm

    private val _updateResult = MutableLiveData<UpdateResult>()
    val updateResult: LiveData<UpdateResult> = _updateResult

    fun emailDataChanged(currentEmail: String, newEmail: String) {
        if (!isEmailValid(newEmail)) {
            _updateForm.value = UpdateFormState(
                    emailError = R.string.error_invalid_email
            )
            return
        }

        if (currentEmail == newEmail) {
            _updateForm.value = UpdateFormState(
                    emailError = R.string.error_email_unchanged
            )
            return
        }

        _updateForm.value = UpdateFormState(
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
                    FtcUser(userId).updateEmail(email)
                }

                _updateResult.value = UpdateResult(
                        success = done
                )
            } catch (e: ClientError) {
                _updateResult.value = UpdateResult(
                        exception = e
                )
            } catch (e: Exception) {
                _updateResult.value = UpdateResult(
                        exception = e
                )
            }
        }
    }

    fun passwordDataChanged(password: String) {

    }

    fun userNameDataChanged(userName: String) {

    }



    fun updateUserName(name: String) {

    }

    fun upatePassword(password: Passwords) {

    }

    fun showProgress(show: Boolean) {
        inProgress.value = show
    }

    fun setDone(ok: Boolean) {
        done.value = ok
    }
}
