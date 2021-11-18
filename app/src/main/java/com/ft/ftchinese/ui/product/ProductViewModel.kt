package com.ft.ftchinese.ui.product

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.paywall.CheckoutPrice
import com.ft.ftchinese.model.paywall.Product
import com.ft.ftchinese.model.paywall.Promo
import com.ft.ftchinese.model.reader.Account
import org.jetbrains.anko.AnkoLogger

/**
 * Used by ProductFragment to pass information to host
 * activity which product is selected.
 */
class ProductViewModel : ViewModel(), AnkoLogger {

    val checkoutItemSelected: MutableLiveData<CheckoutPrice> by lazy {
        MutableLiveData<CheckoutPrice>()
    }

    val inputEnabled: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val accountChanged: MutableLiveData<Account?> by lazy {
        MutableLiveData<Account?>()
    }

    // When host activity retrieved paywall data, pass the raw products to ProductFragment.
    val productsReceived: MutableLiveData<List<Product>> by lazy {
        MutableLiveData<List<Product>>()
    }

    // When the host activity retrieved paywall data, convert the promo field to PromoUI
    val promoCreated: MutableLiveData<Promo> by lazy {
        MutableLiveData<Promo>()
    }
}
