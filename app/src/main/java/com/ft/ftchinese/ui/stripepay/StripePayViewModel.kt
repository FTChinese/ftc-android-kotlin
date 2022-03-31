package com.ft.ftchinese.ui.stripepay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.fetch.FetchUi
import com.ft.ftchinese.model.paywall.CartItemStripeV2
import com.ft.ftchinese.model.reader.Account

private const val TAG = "StripePayViewModel"

class StripePayViewModel : ViewModel() {

    val isNetworkAvailable = MutableLiveData<Boolean>()

    private var _progress = MutableLiveData<FetchUi>(FetchUi.Progress(false))
    val progressLiveData = _progress

    fun subscribe(account: Account, item: CartItemStripeV2) {

    }

    private fun createSubs(account: Account) {

    }

    private fun updateSubs(account: Account) {

    }
}
