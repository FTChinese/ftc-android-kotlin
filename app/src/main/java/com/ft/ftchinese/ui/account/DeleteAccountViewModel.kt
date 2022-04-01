package com.ft.ftchinese.ui.account

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.EmailPasswordParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeleteAccountViewModel : BaseViewModel() {
    val passwordLiveData = MutableLiveData("")
    val passwordValidator = LiveDataValidator(passwordLiveData).apply {
        addRule("密码必填", Validator::notEmpty)
    }

    private val isDirty: Boolean
        get() = passwordValidator.isDirty()

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(passwordLiveData) {
            value = enableForm()
        }
        addSource(progressLiveData) {
            value = enableForm()
        }
    }

    init {
        progressLiveData.value = false
    }

    private fun enableForm(): Boolean {
        return progressLiveData.value == false && isDirty && passwordValidator.isValid()
    }

    val deleted: MutableLiveData<FetchResult<Boolean>> by lazy {
        MutableLiveData<FetchResult<Boolean>>()
    }

    val validSubsExists: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    fun deleteAccount(a: Account) {
        if (isNetworkAvailable.value != true) {
            deleted.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        val c = EmailPasswordParams(
            email = a.email,
            password = passwordLiveData.value?.trim() ?: "",
        )

        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.deleteAccount(
                        ftcId = a.id,
                        params = c,
                    )
                }

                progressLiveData.value = false
                deleted.value = FetchResult.Success(done)

            } catch (e: APIError) {
                progressLiveData.value = false
                handleApiErr(e)

            } catch (e: Exception) {
                progressLiveData.value = false

                deleted.value = FetchResult.fromException(e)
            }
        }
    }

    private fun handleApiErr(e: APIError) {
        when (e.statusCode) {
            403 -> {
                deleted.value = FetchResult.LocalizedError(R.string.password_not_verified)
                return
            }
            404 -> {
                deleted.value = FetchResult.LocalizedError(R.string.account_not_found)
                return
            }
            422 -> {
                when {
                    e.error == null -> {
                        deleted.value = FetchResult.fromApi(e)
                        return
                    }
                    e.error.isFieldMissing("email") -> {
                        deleted.value = FetchResult.LocalizedError(R.string.message_delete_email_mismatch)
                        return
                    }
                    e.error.isFieldAlreadyExists("subscription") -> {
                        validSubsExists.value = true
                        return
                    }
                    else -> {
                        deleted.value = FetchResult.fromApi(e)
                    }
                }
            }
        }
    }
}
