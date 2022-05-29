package com.ft.ftchinese.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ConnectionLiveData
import com.ft.ftchinese.ui.base.ToastMessage

open class BaseAppViewModel(application: Application) : AndroidViewModel(application) {
    val connectionLiveData = ConnectionLiveData(application)
    val progressLiveData = MutableLiveData(false)
    val refreshingLiveData: MutableLiveData<Boolean> by lazy {
        MutableLiveData(false)
    }
    val toastLiveData: MutableLiveData<ToastMessage> by lazy {
        MutableLiveData<ToastMessage>()
    }

    fun resetToast() {
        toastLiveData.value = null
    }

    fun ensureNetwork(): Boolean {
        if (connectionLiveData.value != true) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return false
        }

        return true
    }
}
