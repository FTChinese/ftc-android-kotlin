package com.ft.ftchinese.ui.ftcpay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.ui.product.PriceCard
import com.ft.ftchinese.ui.product.PriceCardParams
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun FtcPayScreen() {

}

@Composable
fun FtcPayBody(
    cartItem: CartItemFtcV2,
) {
    Column {
        Card {
            PriceCard(
                params = PriceCardParams.ofFtc(LocalContext.current, cartItem)
            )

        }

        Text(
            text = cartItem.intent.message,
            style = MaterialTheme.typography.body2,
            color = OColor.claret
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))
    }
}
