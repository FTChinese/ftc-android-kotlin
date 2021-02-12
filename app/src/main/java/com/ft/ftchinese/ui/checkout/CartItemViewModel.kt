package com.ft.ftchinese.ui.checkout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CartItemViewModel : ViewModel() {
//    val cartItem = MutableLiveData<CartItem>()
    val discountsFound: MutableLiveData<DiscountSpinnerParams> by lazy {
        MutableLiveData<DiscountSpinnerParams>()
    }
    val priceSelected: MutableLiveData<ProductPriceParams> by lazy {
        MutableLiveData<ProductPriceParams>()
    }
    val discountChanged: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
}
