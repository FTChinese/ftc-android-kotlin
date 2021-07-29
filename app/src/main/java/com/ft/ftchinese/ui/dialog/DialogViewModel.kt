package com.ft.ftchinese.ui.dialog

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DialogViewModel :  ViewModel() {
    val positiveButtonClicked = MutableLiveData<Boolean>()
    val negativeButtonClicked = MutableLiveData<Boolean>()
}
