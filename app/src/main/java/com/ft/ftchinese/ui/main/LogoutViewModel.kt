package com.ft.ftchinese.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

@Deprecated("")
class LogoutViewModel : ViewModel() {
    val loggedOutLiveData: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    fun logout() {
        loggedOutLiveData.value = true
    }
}
