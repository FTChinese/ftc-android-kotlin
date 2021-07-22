package com.ft.ftchinese.ui.webpage

import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.ui.base.BaseViewModel

class WebpageViewModel : BaseViewModel() {
    /**
     * Tell [WebpageFragment] to load a url directly.
     */
    val urlLiveData: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}
