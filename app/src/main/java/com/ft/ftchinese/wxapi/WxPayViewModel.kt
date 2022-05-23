package com.ft.ftchinese.wxapi

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.reader.Membership

sealed class WxPayStatus {
    object Loading: WxPayStatus()
    data class Success(val membership: Membership): WxPayStatus()
    data class Error(val message: String): WxPayStatus()
    object Canceled: WxPayStatus()
}

class WxPayViewModel : ViewModel() {

    val statusLiveData: MutableLiveData<WxPayStatus> by lazy {
        MutableLiveData<WxPayStatus>()
    }

    fun onPayStatus(status: WxPayStatus) {
        statusLiveData.value = status
    }
}
