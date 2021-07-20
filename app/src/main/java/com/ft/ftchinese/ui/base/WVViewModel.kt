package com.ft.ftchinese.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.content.ChannelSource

class WVViewModel : ViewModel() {
    // When a url in webview is clicked and the url point to
    // another channel page.
    val urlChannelSelected: MutableLiveData<ChannelSource> by lazy {
        MutableLiveData<ChannelSource>()
    }

    val pageFinished: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val pagingBtnClicked: MutableLiveData<Paging> by lazy {
        MutableLiveData<Paging>()
    }

    val openGraphEvaluated: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}
