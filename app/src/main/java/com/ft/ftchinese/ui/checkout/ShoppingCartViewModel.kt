package com.ft.ftchinese.ui.checkout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.model.paywall.CartItemStripeV2

sealed class ShoppingCart {
    object Empty : ShoppingCart()
    class Ftc(val item: CartItemFtcV2) : ShoppingCart()
    class Stripe(val item: CartItemStripeV2) : ShoppingCart()
}

class ShoppingCartViewModel : ViewModel() {
    val itemLiveData: MutableLiveData<CartItem> by lazy {
        MutableLiveData<CartItem>()
    }

    private var _cart by mutableStateOf<ShoppingCart>(ShoppingCart.Empty)

    val cart = _cart

    val isEmptyCart: Boolean
        get() = _cart is ShoppingCart.Empty

    fun putFtcItem(item: CartItemFtcV2) {
        _cart = ShoppingCart.Ftc(item = item)
    }

    fun putStripeItem(item: CartItemStripeV2) {
        _cart = ShoppingCart.Stripe(item = item)
    }

    fun clear() {
        _cart = ShoppingCart.Empty
    }
}
