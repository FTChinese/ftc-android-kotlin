package com.ft.ftchinese.ui.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UpdateViewModel : ViewModel() {
    val inProgress = MutableLiveData<Boolean>()
    val done = MutableLiveData<Boolean>()

    fun showProgress(show: Boolean) {
        inProgress.value = show
    }

    fun setDone(ok: Boolean) {
        done.value = ok
    }
}
