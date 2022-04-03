package com.ft.ftchinese.ui.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PriceList(
    ftcCartItems: List<CartItemFtc>,
    stripeCartItems: List<CartItemStripe>,
    onFtcPay: (item: CartItemFtc) -> Unit,
    onStripePay: (item: CartItemStripe) -> Unit,
) {
    Column {
        ftcCartItems.forEach { cartItem ->
            Card(
                onClick = { onFtcPay(cartItem) },
                backgroundColor = OColor.wheat,
                elevation = Dimens.dp4
            ) {
                PriceCard(
                    params = PriceCardParams.ofFtc(LocalContext.current, cartItem),
                )
            }

            Spacer(modifier = Modifier.height(Dimens.dp16))
        }

        stripeCartItems.forEach { cartItem ->
            Card(
                onClick = { onStripePay(cartItem) },
                backgroundColor = OColor.wheat,
                elevation = Dimens.dp4,
            ) {
                PriceCard(
                    params = PriceCardParams.ofStripe(LocalContext.current, cartItem),
                )
            }

            Spacer(modifier = Modifier.height(Dimens.dp16))
        }
    }
}
