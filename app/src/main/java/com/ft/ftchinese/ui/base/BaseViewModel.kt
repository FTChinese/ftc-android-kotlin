package com.ft.ftchinese.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel: ViewModel() {
    val progressLiveData = MutableLiveData<Boolean>()
    val isNetworkAvailable = MutableLiveData<Boolean>()
}
