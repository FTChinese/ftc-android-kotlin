package com.ft.ftchinese.ui.account.address

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.Validator

class DeleteAccountViewModel : ViewModel() {
    val progressLiveData = MutableLiveData<Boolean>()
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
}
