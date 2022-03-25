package com.ft.ftchinese.ui.paywall

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.model.paywall.CartItemStripeV2
import com.ft.ftchinese.ui.product.PriceCard
import com.ft.ftchinese.ui.product.PriceCardParams

@Composable
fun PriceList(
    ftcCartItems: List<CartItemFtcV2>,
    stripeCartItems: List<CartItemStripeV2>,
    onFtcPay: (item: CartItemFtcV2) -> Unit,
    onStripePay: (item: CartItemStripeV2) -> Unit,
) {
    Column {
        ftcCartItems.forEach { cartItem ->
            PriceCard(
                params = PriceCardParams.ofFtc(LocalContext.current, cartItem),
                onClick = { onFtcPay(cartItem) }
            )
        }

        stripeCartItems.forEach { cartItem ->
            PriceCard(
                params = PriceCardParams.ofStripe(LocalContext.current, cartItem),
                onClick = { onStripePay(cartItem) },
            )
        }
    }
}
