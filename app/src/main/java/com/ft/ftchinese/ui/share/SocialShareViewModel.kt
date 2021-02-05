package com.ft.ftchinese.ui.share

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SocialShareViewModel : ViewModel() {
    val appSelected = MutableLiveData<SocialApp>()

    fun select(app: SocialApp) {
        appSelected.value = app
    }
}
