package com.ft.ftchinese.ui.product

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.paywall.Banner
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.PaywallProduct
import com.ft.ftchinese.model.reader.Account

/**
 * Used by ProductFragment to pass information to host
 * activity which product is selected.
 */
class ProductViewModel : ViewModel() {

    val checkoutItemSelected: MutableLiveData<CartItemFtc> by lazy {
        MutableLiveData<CartItemFtc>()
    }

    val accountChanged: MutableLiveData<Account?> by lazy {
        MutableLiveData<Account?>()
    }

    // When host activity retrieved paywall data, pass the raw products to ProductFragment.
    val productsReceived: MutableLiveData<List<PaywallProduct>> by lazy {
        MutableLiveData<List<PaywallProduct>>()
    }

    // When the host activity retrieved paywall data, convert the promo field to PromoUI
    val promoReceived: MutableLiveData<Banner> by lazy {
        MutableLiveData<Banner>()
    }
}
