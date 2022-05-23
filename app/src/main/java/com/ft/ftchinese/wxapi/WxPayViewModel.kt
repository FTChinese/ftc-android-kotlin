package com.ft.ftchinese.wxapi

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.reader.Membership

class WxPayViewModel : ViewModel() {

    val statusLiveData: MutableLiveData<WxPayStatus> by lazy {
        MutableLiveData<WxPayStatus>()
    }

    fun onPayStatus(status: WxPayStatus) {
        statusLiveData.value = status
    }
}
