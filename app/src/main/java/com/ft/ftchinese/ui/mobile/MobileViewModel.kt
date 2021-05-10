package com.ft.ftchinese.ui.mobile

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.model.request.MobileAuthParams
import com.ft.ftchinese.model.request.MobileFormParams
import com.ft.ftchinese.model.request.SMSCodeParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class MobileViewModel : BaseViewModel(), AnkoLogger {

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

    private val isDirty: Boolean
        get() = !mobileLiveData.value.isNullOrBlank() && !codeLiveData.value.isNullOrBlank()

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

    private fun enableCodeRequest(): Boolean {
        if (counterLiveData.value != 0) {
            return false
        }
        return progressLiveData.value == false && !mobileLiveData.value.isNullOrBlank() && mobileValidator.isValid()
    }

    private fun enableForm(): Boolean {
        return progressLiveData.value == false && isDirty && formValidator.isValid()
    }

    private fun startCounting() {
        viewModelScope.launch(Dispatchers.Main) {
            for (i in 60 downTo 0) {
                counterLiveData.value = i
                delay(1000)
            }
        }
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

    private val updateMobileParams: MobileFormParams
        get() = MobileFormParams(
            mobile = mobileLiveData.value ?: "",
            code = codeLiveData.value ?: "",
        )

    val codeSent: MutableLiveData<FetchResult<Boolean>> by lazy {
        MutableLiveData<FetchResult<Boolean>>()
    }

    val mobileUpdated: MutableLiveData<FetchResult<BaseAccount>> by lazy {
        MutableLiveData<FetchResult<BaseAccount>>()
    }

    // If the mobile is used to login for the first time.
    val mobileNotSet: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val accountLoaded: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    // Send SMS for login.
    fun requestSMSAuthCode() {
        if (isNetworkAvailable.value != true) {
            codeSent.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        startCounting()

        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    AuthClient.requestSMSCode(smsCodeParams)
                }

                if (ok) {
                    codeSent.value = FetchResult.Success(true)
                } else {
                    codeSent.value = FetchResult.Error(Exception("Unknown error occurred!"))
                }

                progressLiveData.value = false
            } catch (e: Exception) {
                codeSent.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }

    // Perform login after SMS cod entered.
    // Server returns UserFound object.
    // If the id field is null, launch a ui to enter email
    // so that we could link this mobile to an email account;
    // otherwise use the id to fetch account data.
    fun verifySMSAuthCode(deviceToken: String) {
        if (isNetworkAvailable.value != true) {
            accountLoaded.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        val params = MobileAuthParams(
            mobile = mobileLiveData.value ?: "",
            code = codeLiveData.value ?: "",
            deviceToken = deviceToken
        )

        viewModelScope.launch {
            try {
                val found = withContext(Dispatchers.IO) {
                    AuthClient.verifySMSCode(params)
                }

                if (found == null) {
                    accountLoaded.value = FetchResult.LocalizedError(R.string.error_unknown)
                    progressLiveData.value = false
                    return@launch
                }

                if (found.id == null) {
                    progressLiveData.value = false
                    mobileNotSet.value = true
                    return@launch
                }

                loadByFtcID(found.id)
            } catch (e: Exception) {
                accountLoaded.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }

    private suspend fun loadByFtcID(id: String) {

        try {
            val account = withContext(Dispatchers.IO) {
                AccountRepo.loadFtcAccount(id)
            }

            if (account == null) {
                accountLoaded.value = FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                accountLoaded.value = FetchResult.Success(account)
            }
            progressLiveData.value = false
        } catch (e: ClientError) {
            progressLiveData.value = false
            accountLoaded.value = if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.api_account_not_found)
            } else {
                FetchResult.fromServerError(e)
            }
        } catch (e: Exception) {
            accountLoaded.value = FetchResult.fromException(e)
            progressLiveData.value = false
        }
    }

    // Request code for update after logged-in
    fun requestCodeForUpdate(account: Account) {
        if (isNetworkAvailable.value != true) {
            codeSent.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        info("Requesting code for ${mobileLiveData.value}")
        progressLiveData.value = true
        startCounting()

        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    AccountRepo.requestSMSCode(account, smsCodeParams)
                }

                if (ok) {
                    codeSent.value = FetchResult.Success(true)
                } else {
                    codeSent.value = FetchResult.Error(Exception("Unknown error occurred!"))
                }

                progressLiveData.value = false
            } catch (e: Exception) {
                codeSent.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }

    // Update mobile in settings.
    fun updateMobile(account: Account) {
        if (isNetworkAvailable.value != true) {
            mobileUpdated.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        info("Mobile ${mobileLiveData.value}, code ${codeLiveData.value}")

        viewModelScope.launch {
            try {
                val baseAccount = withContext(Dispatchers.IO) {
                    AccountRepo.updateMobile(account,updateMobileParams)
                }

                if (baseAccount == null) {
                    mobileUpdated.value = FetchResult.LocalizedError(R.string.error_unknown)
                } else {
                    mobileUpdated.value = FetchResult.Success(baseAccount)
                }

                progressLiveData.value = false
            } catch (e: Exception) {
                mobileUpdated.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }
}
