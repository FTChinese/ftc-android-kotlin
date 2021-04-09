package com.ft.ftchinese.ui.mobile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.validator.Validator
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger

class MobileViewModel : ViewModel(), AnkoLogger {

    val mobileNumber = MutableLiveData("")
    val verificationCode = MutableLiveData("")

    private var mobile = ""
    private var code = ""
    private var prevState = MobileFormState()

    val isNetworkAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val formState: MutableLiveData<MobileFormState> by lazy {
        MutableLiveData<MobileFormState>()
    }

    val codeSent: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    val mobileUpdated: MutableLiveData<Result<BaseAccount>> by lazy {
        MutableLiveData<Result<BaseAccount>>()
    }

    fun validateMobile(m: String) {
        if (m == AccountCache.get()?.mobile) {
            return
        }

        mobile = m

        if (!Validator.isMainlandPhone(m)) {
            formState.value = MobileFormState(
                errMsg = R.string.error_invalid_mobile,
                errField = MobileFormField.Phone,
                mobileValid = false,
                codeValid = prevState.codeValid,
            )

            return
        }

        formState.value = MobileFormState(
            errMsg = null,
            errField = null,
            mobileValid = true,
            codeValid = prevState.codeValid,
        )
    }

    fun validateCode(c: String) {
        code = c
        if (!Validator.validateCode(c)) {
            formState.value = MobileFormState(
                errMsg = R.string.error_invalid_code,
                errField = MobileFormField.Code,
                mobileValid = prevState.mobileValid,
                codeValid = false,
            )
            return
        }

        formState.value = MobileFormState(
            errMsg = null,
            errField = null,
            mobileValid = prevState.mobileValid,
            codeValid = true,
        )
    }

    fun requestCode(account: Account) {
        if (isNetworkAvailable.value != true) {
            codeSent.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {

                codeSent.value = Result.Success(true)
            } catch (e: Exception) {
                codeSent.value = parseException(e)
            }
        }
    }

    fun updateMobile(account: Account) {
        if (isNetworkAvailable.value != true) {
            mobileUpdated.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {

            } catch (e: Exception) {
                mobileUpdated.value = parseException(e)
            }
        }
    }
}
