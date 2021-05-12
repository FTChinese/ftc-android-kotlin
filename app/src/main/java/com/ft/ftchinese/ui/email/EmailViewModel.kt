package com.ft.ftchinese.ui.email

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ServerError
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger

class EmailViewModel : BaseViewModel(), AnkoLogger{

    val emailLiveData = MutableLiveData("")
    val emailValidator = LiveDataValidator(emailLiveData).apply {
        addRule("请输入正确的邮箱", Validator::isEmail)
        addRule("不能使用当前邮箱") {
            AccountCache.get()?.email != it
        }
    }

    private val isDirty: Boolean
        get() = !emailLiveData.value.isNullOrBlank()

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enableForm()
        }
        addSource(emailLiveData) {
            value = enableForm()
        }
    }

    val isLetterBtnEnabled = MutableLiveData(true)

    init {
        progressLiveData.value = false
    }

    private fun enableForm(): Boolean {
        return progressLiveData.value == false && isDirty && emailValidator.isValid()
    }

    val existsResult: MutableLiveData<FetchResult<Boolean>> by lazy {
        MutableLiveData<FetchResult<Boolean>>()
    }

    val emailUpdated: MutableLiveData<FetchResult<BaseAccount>> by lazy {
        MutableLiveData<FetchResult<BaseAccount>>()
    }

    val letterSent: MutableLiveData<FetchResult<Boolean>> by lazy {
        MutableLiveData<FetchResult<Boolean>>()
    }

    fun checkExists() {
        if (isNetworkAvailable.value != true) {
            existsResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
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

                existsResult.value = FetchResult.Success(ok)
                progressLiveData.value = false
            } catch (e: ServerError) {
                progressLiveData.value = false

                if (e.statusCode == 404) {
                    existsResult.value = FetchResult.Success(false)
                    return@launch
                }

                existsResult.value = FetchResult.fromServerError(e)
            } catch (e: Exception) {
                existsResult.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }

    fun updateEmail(userId: String) {

        if (isNetworkAvailable.value != true) {
            existsResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        val email = emailLiveData.value ?: ""
        viewModelScope.launch {
            try {
                val baseAccount = withContext(Dispatchers.IO) {
                    AccountRepo.updateEmail(userId, email)
                }

                progressLiveData.value = false
                if (baseAccount == null) {
                    emailUpdated.value = FetchResult.LocalizedError(R.string.error_unknown)
                } else {
                    emailUpdated.value = FetchResult.Success(baseAccount)
                    clear()
                }
            } catch (e: ServerError) {
                progressLiveData.value = false

                emailUpdated.value = when (e.statusCode) {
                    422 -> {
                        if (e.error == null) {
                            FetchResult.fromServerError(e)
                        } else {
                            when {
                                e.error.isFieldAlreadyExists("email") -> FetchResult.LocalizedError(R.string.signup_email_taken)
                                e.error.isFieldInvalid("email") -> FetchResult.LocalizedError(R.string.signup_invalid_email)
                                else -> FetchResult.fromServerError(e)
                            }
                        }
                    }
                    else -> FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                emailUpdated.value = FetchResult.fromException(e)
            }
        }
    }

    fun requestVerification() {
        if (isNetworkAvailable.value != true) {
            letterSent.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        val userId = AccountCache.get()?.id ?: return
        progressLiveData.value = true
        isLetterBtnEnabled.value = false

        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.requestVerification(userId)
                }

                letterSent.value = FetchResult.Success(done)

                progressLiveData.value = false
                isLetterBtnEnabled.value = !done
            } catch (e: ServerError) {
                val msgId = when (e.statusCode) {
                    404 -> R.string.account_not_found
                    422 -> if (e.error?.isResourceMissing("email_server") == true) {
                        R.string.api_email_server_down
                    } else null
                    else -> null
                }

                letterSent.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromServerError(e)
                }
                progressLiveData.value = false
                isLetterBtnEnabled.value = true
            } catch (e: Exception) {
                letterSent.value = FetchResult.fromException(e)
                progressLiveData.value = false
                isLetterBtnEnabled.value = true
            }
        }
    }

    fun clear() {
        emailLiveData.value = ""
    }
}
