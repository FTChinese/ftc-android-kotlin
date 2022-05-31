package com.ft.ftchinese.ui.subs.ftcpay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.ftcsubs.AliPayIntent

data class AliPayResult(
    val intent: AliPayIntent,
    val result: Map<String, String>
)

class FtcPayViewModel : ViewModel() {

    val aliPayResult: MutableLiveData<AliPayResult> by lazy {
        MutableLiveData<AliPayResult>()
    }

    fun setAliPayResult(r:AliPayResult) {
        aliPayResult.value = r
    }

    fun clear() {
        aliPayResult.value = null
    }
}
