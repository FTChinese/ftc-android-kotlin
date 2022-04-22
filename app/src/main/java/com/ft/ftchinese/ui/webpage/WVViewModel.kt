package com.ft.ftchinese.ui.webpage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WVViewModel : ViewModel() {

    // Used inside article activity.
    val pageFinished: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val openGraphEvaluated: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val htmlReceived: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val urlLiveData: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}
