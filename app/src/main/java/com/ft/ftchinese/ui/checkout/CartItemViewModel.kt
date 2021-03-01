package com.ft.ftchinese.ui.checkout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.price.CheckoutItem

class CartItemViewModel : ViewModel() {
//    val cartItem = MutableLiveData<CartItem>()
    val discountsFound: MutableLiveData<DiscountSpinnerParams> by lazy {
        MutableLiveData<DiscountSpinnerParams>()
    }
    val priceInCart: MutableLiveData<CheckoutItem> by lazy {
        MutableLiveData<CheckoutItem>()
    }
    val discountChanged: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
}
