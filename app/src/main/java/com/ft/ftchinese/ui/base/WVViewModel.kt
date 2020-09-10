package com.ft.ftchinese.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.content.ChannelSource

class WVViewModel () : ViewModel() {
    // When a url in webview is clicked and the url point to
    // another channel page.
    val urlChannelSelected: MutableLiveData<ChannelSource> by lazy {
        MutableLiveData<ChannelSource>()
    }
}
