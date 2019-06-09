package com.ft.ftchinese.ui.pay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.PayMethod

/**
 * Used by [PayFragment] to tell [CheckOutActivity] that the
 * pay button is clicked.
 * [CheckOutActivity] also use this to tell [PayFragment] to
 * re-enable the pay button in case of errors when calling
 * API or wechat/alipay.
 */
class CheckOutViewModel : ViewModel() {

    val methodSelected = MutableLiveData<PayMethod>()
    val payStarted = MutableLiveData<PayMethod>()
    val directUpgrade = MutableLiveData<Boolean>()
    val enabled = MutableLiveData<Boolean>()

    fun selectPayMethod(method: PayMethod) {
        methodSelected.value = method
    }

    // User clicked the pay/upgrade button
    fun startPayment(method: PayMethod) {
        payStarted.value = method
    }

    fun startUpgrading(v: Boolean) {
        directUpgrade.value = true
    }

    // Enable/Disable a UI, like button.
    fun enableInput(v: Boolean) {
        enabled.value = v
    }
}
