package com.ft.ftchinese.ui.checkout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.ui.product.PriceCardParams

class ShoppingCartViewModel : ViewModel() {
    val itemLiveData: MutableLiveData<PriceCardParams> by lazy {
        MutableLiveData<PriceCardParams>()
    }
}
