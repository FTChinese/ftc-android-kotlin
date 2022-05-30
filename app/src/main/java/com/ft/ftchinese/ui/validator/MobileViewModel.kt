package com.ft.ftchinese.ui.validator

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "MobileViewModel"

class MobileViewModel : BaseViewModel() {

    val mobileLiveData = MutableLiveData("")
    val mobileValidator = LiveDataValidator(mobileLiveData).apply {
        addRule("请输入正确的手机号码", Validator::isMainlandPhone)

        addRule("手机已设置") {
            // If the input number is the the same as the current one.
            AccountCache.get()?.mobile != it
        }
    }

    val codeLiveData = MutableLiveData("")
    val codeValidator = LiveDataValidator(codeLiveData).apply {
        addRule("请输入验证码", Validator.minLength(6))
    }

    val counterLiveData = MutableLiveData<Int>()
    private var job: Job? = null

    private val formValidator = LiveDataValidatorResolver(listOf(mobileValidator, codeValidator))

    val isCodeRequestEnabled = MediatorLiveData<Boolean>().apply {
        addSource(mobileLiveData) {
            value = enableCodeRequest()
            Log.i(TAG, "isCodeRequestEnabled : mobileLiveData : $value")
        }
        addSource(progressLiveData) {
            value = enableCodeRequest()
            Log.i(TAG, "isCodeRequestEnabled : progressLiveData : $value")
        }
        addSource(counterLiveData) {
            value = enableCodeRequest()
            Log.i(TAG, "isCodeRequestEnabled : counterLiveData : $value")
        }
    }

    private fun enableCodeRequest(): Boolean {
        if (progressLiveData.value == true) {
            return false
        }

        if (mobileLiveData.value.isNullOrBlank()) {
            return false
        }

        if (counterLiveData.value != 0) {
            return false
        }

        return mobileValidator.isValid()
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

    private fun enableForm(): Boolean {
        if (progressLiveData.value == true) {
            return false
        }
        if (mobileLiveData.value.isNullOrBlank()) {
            return false
        }
        if (codeLiveData.value.isNullOrBlank()) {
            return false
        }
        return formValidator.isValid()
    }

    private fun startCounting() {
        job = viewModelScope.launch(Dispatchers.Main) {
            for (i in 60 downTo 0) {
                counterLiveData.value = i
                delay(1000)
            }
        }
    }

    private fun clearCounter() {
        job?.cancel()
        counterLiveData.value = 0
    }

    init {
        progressLiveData.value = false
        isFormEnabled.value = false
        counterLiveData.value = 0
        mobileLiveData.value = AccountCache.get()?.mobile
    }
}
