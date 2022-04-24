package com.ft.ftchinese.ui.webpage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.ui.base.Paging

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
