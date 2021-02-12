package com.ft.ftchinese.ui.member

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.reader.Membership

class SubsStatusViewModel : ViewModel() {
    val statusChanged: MutableLiveData<Membership> by lazy {
        MutableLiveData<Membership>()
    }

    val autoRenewWanted: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
}
