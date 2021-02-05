package com.ft.ftchinese.ui.checkout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CartItemViewModel : ViewModel() {
    val cartCreated = MutableLiveData<Cart>()
}
