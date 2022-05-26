package com.ft.ftchinese.ui.mobile

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.model.request.MobileAuthParams
import com.ft.ftchinese.model.request.MobileSignUpParams
import com.ft.ftchinese.model.request.SMSCodeParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.AuthClient
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

    private val smsCodeParams: SMSCodeParams
        get() = SMSCodeParams(
            mobile = mobileLiveData.value ?: "",
        )

    val codeSent: MutableLiveData<FetchResult<Boolean>> by lazy {
        MutableLiveData<FetchResult<Boolean>>()
    }

    val mobileUpdated: MutableLiveData<FetchResult<BaseAccount>> by lazy {
        MutableLiveData<FetchResult<BaseAccount>>()
    }

    // If the mobile is not linked to any existing ftc id,
    // ask user to signup with mobile directly, or link to an email
    // account (either create a email account or provide an existing one).
    val mobileNotSet: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    // If the mobile is already linked to a ftc id,
    // account will be fetched directly.
    val accountLoaded: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    // Send SMS for login.
    fun requestSMSAuthCode() {
        if (isNetworkAvailable.value != true) {
            codeSent.value = FetchResult.notConnected
            return
        }

        progressLiveData.value = true
        startCounting()

        viewModelScope.launch {
             val result = AuthClient.asyncRequestSMSCode(smsCodeParams)
            if (result !is FetchResult.Success) {
                clearCounter()
            }
            codeSent.value = result
            progressLiveData.value = false
        }
    }

    // Perform login after SMS cod entered.
    // Server returns UserFound object.
    // If the id field is null, launch a ui to enter email
    // so that we could link this mobile to an email account;
    // otherwise use the id to fetch account data.
    fun verifySMSAuthCode(deviceToken: String) {
        if (isNetworkAvailable.value != true) {
            accountLoaded.value = FetchResult.notConnected
            return
        }

        progressLiveData.value = true

        val params = MobileAuthParams(
            mobile = mobileLiveData.value ?: "",
            code = codeLiveData.value ?: "",
            deviceToken = deviceToken
        )

        viewModelScope.launch {
            val result = AuthClient.asyncVerifySMSCode(params)

            progressLiveData.value = false

            if (result !is FetchResult.Success) {
                progressLiveData.value = false
                clearCounter()
                return@launch
            }

            val id = result.data.id
            if (id == null) {
                progressLiveData.value = false
                mobileNotSet.value = true
                return@launch
            }

            accountLoaded.value = AccountRepo.asyncLoadFtcAccount(id)
            progressLiveData.value = false
        }
    }

    fun signUp(deviceToken: String) {
        if (isNetworkAvailable.value != true) {
            accountLoaded.value = FetchResult.notConnected
            return
        }

        progressLiveData.value = true

        val params = MobileSignUpParams(
            mobile = mobileLiveData.value ?: "",
            deviceToken = deviceToken,
        )

        Log.i(TAG, "$params")

        viewModelScope.launch {

            accountLoaded.value = AuthClient.asyncMobileSignUp(
                params
            )
            progressLiveData.value = false
        }
    }
}
