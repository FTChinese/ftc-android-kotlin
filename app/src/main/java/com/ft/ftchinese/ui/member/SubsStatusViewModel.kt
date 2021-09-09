package com.ft.ftchinese.ui.member

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SubsStatusViewModel : ViewModel() {
    val statusChanged: MutableLiveData<SubsStatus> by lazy {
        MutableLiveData<SubsStatus>()
    }

    val reactivateStripeRequired: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
}
