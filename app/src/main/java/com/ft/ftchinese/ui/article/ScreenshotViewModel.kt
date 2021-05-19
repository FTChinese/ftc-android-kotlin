package com.ft.ftchinese.ui.article

import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.share.SocialApp

class ScreenshotViewModel: BaseViewModel() {

    val teaserLiveData = MutableLiveData<Teaser>()
    val appSelected: MutableLiveData<SocialApp> by lazy {
        MutableLiveData<SocialApp>()
    }

    fun takeScreenshot(teaser: Teaser) {
        progressLiveData.value = true
        teaserLiveData.value = teaser
    }

    fun shareTo(app: SocialApp) {
        appSelected.value = app
    }
}
