package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProgressViewModel : ViewModel() {
    val inProgress = MutableLiveData<Boolean>()

    init {
        inProgress.value = false
    }

    fun on() {
        inProgress.value = true
    }

    fun off() {
        inProgress.value = false
    }
}
