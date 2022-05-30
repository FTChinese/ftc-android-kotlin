package com.ft.ftchinese.ui.validator

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.base.BaseViewModel

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

    init {
        progressLiveData.value = false
    }

    private fun enableForm(): Boolean {
        return progressLiveData.value == false && isDirty && emailValidator.isValid()
    }

    fun clear() {
        emailLiveData.value = ""
    }
}
