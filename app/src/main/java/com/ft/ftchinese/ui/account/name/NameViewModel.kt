package com.ft.ftchinese.ui.account.name

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NameViewModel : BaseViewModel() {
    val nameLiveData = MutableLiveData("")
    val nameValidator = LiveDataValidator(nameLiveData).apply {
        addRule("过长", Validator.maxLength(32))
        addRule("不能与当前用户名相同") {
            AccountCache.get()?.userName  != it
        }
    }

    private val isDirty: Boolean
        get() = !nameLiveData.value.isNullOrBlank()

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enableForm()
        }
        addSource(nameLiveData) {
            value = enableForm()
        }
    }

    init {
        progressLiveData.value = false
    }

    private fun enableForm(): Boolean {
        return progressLiveData.value == false && isDirty && nameValidator.isValid()
    }

    val nameUpdated: MutableLiveData<FetchResult<BaseAccount>> by lazy {
        MutableLiveData<FetchResult<BaseAccount>>()
    }

    fun update(userId: String) {
        if (isNetworkAvailable.value != true) {
            nameUpdated.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        val name = nameLiveData.value ?: ""
        viewModelScope.launch {
            try {
                val baseAccount = withContext(Dispatchers.IO) {
                    AccountRepo.updateUserName(userId, name)
                }

                progressLiveData.value = false
                if (baseAccount == null) {
                    nameUpdated.value = FetchResult.LocalizedError(R.string.error_unknown)
                } else {
                    nameUpdated.value = FetchResult.Success(baseAccount)
                    clear()
                }
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 422) {
                    when (e.error?.key) {
                        "userName_already_exists" -> R.string.api_name_taken
                        else -> null
                    }
                } else {
                    null
                }

                nameUpdated.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromServerError(e)
                }
                progressLiveData.value = false
            } catch (e: Exception) {
                nameUpdated.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }

    // Clear form data after success.
    fun clear() {
        nameLiveData.value = ""
    }
}
