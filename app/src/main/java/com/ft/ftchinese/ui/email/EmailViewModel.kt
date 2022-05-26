package com.ft.ftchinese.ui.email

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailViewModel : BaseViewModel() {

    val emailLiveData = MutableLiveData("")
    val emailValidator = LiveDataValidator(emailLiveData).apply {
        addRule("请输入完整的邮箱", Validator::isEmail)
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
            } catch (e: APIError) {
                progressLiveData.value = false

                if (e.statusCode == 404) {
                    existsResult.value = FetchResult.Success(false)
                    return@launch
                }

                existsResult.value = FetchResult.fromApi(e)
            } catch (e: Exception) {
                existsResult.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }

    fun clear() {
        emailLiveData.value = ""
    }
}
