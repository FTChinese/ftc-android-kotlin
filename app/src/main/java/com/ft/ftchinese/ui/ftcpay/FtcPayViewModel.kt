package com.ft.ftchinese.ui.ftcpay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.PaywallCache

class FtcPayViewModel : ViewModel() {
    var cartItemState by mutableStateOf<CartItemFtcV2?>(null)
        private set

    var errMsg by mutableStateOf<String?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    fun buildCart(priceId: String, account: Account) {
        // TODO: check if cache exists.
        val price = PaywallCache.findFtcPrice(priceId)
        if (price != null) {
            cartItemState = price
                .buildCartItem(account.membership.normalize())
            return
        }

        errMsg = "Price $priceId not found"

        // TODO: load from server if not found.
    }
}
