package com.ft.ftchinese.ui.checkout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ShoppingCartViewModel : ViewModel() {
    val itemLiveData: MutableLiveData<CartItem> by lazy {
        MutableLiveData<CartItem>()
    }
}
