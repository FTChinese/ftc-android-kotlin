package com.ft.ftchinese.ui.webpage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WVViewModel : ViewModel() {

    @Deprecated("")
    val htmlReceived: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    @Deprecated("")
    val urlLiveData: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}
