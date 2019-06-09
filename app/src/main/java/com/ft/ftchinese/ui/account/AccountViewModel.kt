package com.ft.ftchinese.ui.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.models.LoginMethod

class AccountViewModel : ViewModel() {

    val inProgress = MutableLiveData<Boolean>()
    val loginMethod = MutableLiveData<LoginMethod>()

    fun showProgress(show: Boolean) {
        inProgress.value = show
    }

    fun changeLoginMethod(m: LoginMethod) {
        loginMethod.value = m
    }
}
