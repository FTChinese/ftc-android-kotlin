package com.ft.ftchinese.ui.product

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.paywall.Banner
import com.ft.ftchinese.model.paywall.CheckoutPrice
import com.ft.ftchinese.model.paywall.PaywallProduct
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
    val productsReceived: MutableLiveData<List<PaywallProduct>> by lazy {
        MutableLiveData<List<PaywallProduct>>()
    }

    // When the host activity retrieved paywall data, convert the promo field to PromoUI
    val promoReceived: MutableLiveData<Banner> by lazy {
        MutableLiveData<Banner>()
    }
}
