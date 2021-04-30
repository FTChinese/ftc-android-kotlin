package com.ft.ftchinese.ui.login

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.Validator
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseApiError
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger

class EmailExistsViewModel : ViewModel(), AnkoLogger{
    // Used to toggle button enabled state only.
    // Progress indicator is controlled by hosting activity.
    val progressLiveData = MutableLiveData<Boolean>()
    val isNetworkAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val emailLiveData = MutableLiveData("")
    val emailValidator = LiveDataValidator(emailLiveData).apply {
        addRule("请输入正确的邮箱", Validator::isEmail)
    }

    private val isDirty: Boolean
        get() = !emailLiveData.value.isNullOrBlank()

    val isFormEnabled = MediatorLiveData<Boolean>()

    init {
        progressLiveData.value = false
        isFormEnabled.addSource(progressLiveData) {
            toggleForm()
        }
        isFormEnabled.addSource(emailLiveData) {
            toggleForm()
        }
    }

    private fun toggleForm() {
        isFormEnabled.value = progressLiveData.value == false && isDirty && emailValidator.isValid()
    }

    val existsResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    fun startChecking() {
        if (isNetworkAvailable.value != true) {
            existsResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    emailLiveData.value?.let {
                        AuthClient.emailExists(it)
                    } ?: false
                }

                existsResult.value = Result.Success(ok)
                progressLiveData.value = false
            } catch (e: ClientError) {
                progressLiveData.value = false

                if (e.statusCode == 404) {
                    existsResult.value = Result.Success(false)
                    return@launch
                }

                existsResult.value = parseApiError(e)
            } catch (e: Exception) {
                existsResult.value = parseException(e)
                progressLiveData.value = false
            }
        }
    }
}
