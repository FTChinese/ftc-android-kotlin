package com.ft.ftchinese.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.content.ChannelSource

class WVViewModel : ViewModel() {
    // When a url in webview is clicked and the url point to
    // another channel page.
    // Use inside channel fragment
    val urlChannelSelected: MutableLiveData<ChannelSource> by lazy {
        MutableLiveData<ChannelSource>()
    }

    val pagingBtnClicked: MutableLiveData<Paging> by lazy {
        MutableLiveData<Paging>()
    }

    // Used inside article activity.
    val pageFinished: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val openGraphEvaluated: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}
