package com.ft.ftchinese.ui.mobile

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class MobileViewModel : ViewModel(), AnkoLogger {

    val mobileLiveData = MutableLiveData("")
    val mobileValidator = LiveDataValidator(mobileLiveData).apply {
        addRule("请输入手机号码") {
            it.isNullOrBlank()
        }
        addRule("请输入正确的手机号码") {
            !Validator.isMainlandPhone(it)
        }
        addRule("") {
            // If the input number is the the same as the current one.
            AccountCache.get()?.mobile == it
        }
    }

    val codeLiveData = MutableLiveData("")
    val codeValidator = LiveDataValidator(codeLiveData).apply {
        addRule("请输入验证码") { it.isNullOrBlank()}
    }

    // Used to toggle button enabled state only.
    // Progress indicator is controlled by hosting activity.
    val progressLiveData = MutableLiveData<Boolean>()

    val counterLiveData = MutableLiveData<Int>()

    private val formValidator = LiveDataValidatorResolver(listOf(mobileValidator, codeValidator))

    val isCodeRequestEnabled = MediatorLiveData<Boolean>().apply {
        addSource(mobileLiveData) {
            value = enableCodeRequest()
            info("isCodeRequestEnabled : mobileLiveData : $value")
        }
        addSource(progressLiveData) {
            value = enableCodeRequest()
            info("isCodeRequestEnabled : progressLiveData : $value")
        }
        addSource(counterLiveData) {
            value = enableCodeRequest()
            info("isCodeRequestEnabled : counterLiveData : $value")
        }
    }

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(mobileLiveData) {
            value = enableForm()
        }
        addSource(codeLiveData) {
            value = enableForm()
        }
        addSource(progressLiveData) {
            value = enableForm()
        }
    }

    init {
        progressLiveData.value = false
        isFormEnabled.value = false
        counterLiveData.value = 0
    }

    private fun enableCodeRequest(): Boolean {
        if (counterLiveData.value != 0) {
            return false
        }
        return progressLiveData.value == false && mobileValidator.isValid()
    }

    private fun enableForm(): Boolean {
        return progressLiveData.value == false && formValidator.isValid()
    }

    val isNetworkAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val codeSent: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    val mobileUpdated: MutableLiveData<Result<BaseAccount>> by lazy {
        MutableLiveData<Result<BaseAccount>>()
    }

    private fun startCounting() {
        viewModelScope.launch(Dispatchers.Main) {
            for (i in 60 downTo 0) {
                counterLiveData.value = i
                delay(1000)
            }
        }
    }

    fun requestCode(account: Account) {
        if (isNetworkAvailable.value != true) {
            codeSent.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        info("Requesting code for ${mobileLiveData.value}")
        progressLiveData.value = true
        startCounting()

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    for (i in 0 until 10) {
                        delay(1000)
                    }
                }
                codeSent.value = Result.Success(true)
                progressLiveData.value = false
            } catch (e: Exception) {
                codeSent.value = parseException(e)
                progressLiveData.value = false
            }
        }
    }

    fun updateMobile(account: Account) {
        if (isNetworkAvailable.value != true) {
            mobileUpdated.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        info("Mobile ${mobileLiveData.value}, code ${codeLiveData.value}")

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    for (i in 0 until 10) {
                        delay(1000)
                    }
                }
                mobileUpdated.value = Result.Success(BaseAccount(
                    id = "",
                    unionId = null,
                    stripeId = null,
                    email = "",
                    mobile = null,
                    userName = null,
                    avatarUrl = null,
                    isVerified = false,
                ))
                progressLiveData.value = false
            } catch (e: Exception) {
                mobileUpdated.value = parseException(e)
                progressLiveData.value = false
            }
        }
    }
}
