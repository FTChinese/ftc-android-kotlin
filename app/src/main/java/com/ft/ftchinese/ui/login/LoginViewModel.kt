package com.ft.ftchinese.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    val email = MutableLiveData<Pair<String, Boolean>>()
    val inProgress = MutableLiveData<Boolean>()
    val userId = MutableLiveData<String>()

    fun foundEmail(result: Pair<String, Boolean>) {
        this.email.value = result
    }

    fun showProgress(show: Boolean) {
        inProgress.value = show
    }

    fun serUserId(id: String) {
        userId.value = id
    }
}