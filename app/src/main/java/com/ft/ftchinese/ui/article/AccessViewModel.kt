package com.ft.ftchinese.ui.article

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.reader.Access

class AccessViewModel : ViewModel() {
    val accessLiveData: MutableLiveData<Access> by lazy {
        MutableLiveData<Access>()
    }
}
