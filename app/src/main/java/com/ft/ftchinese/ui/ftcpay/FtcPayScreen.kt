package com.ft.ftchinese.ui.ftcpay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CheckoutIntent
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.product.PriceCard
import com.ft.ftchinese.ui.product.PriceCardParams
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun FtcPayScreen(
    cartItem: CartItemFtc,
    loading: Boolean,
    onClickPay: (PayMethod) -> Unit,
) {
    val context = LocalContext.current

    val radioOptions = listOf(PayMethod.ALIPAY, PayMethod.WXPAY)
    val (selectOption, onOptionSelected) = remember {
        mutableStateOf<PayMethod?>(null)
    }

    val forbidden = cartItem.intent.kind == IntentKind.Forbidden

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16),
    ) {
        Column(
            modifier = Modifier
                .weight(1.0f)
        ) {

            CheckoutHeader(tier = cartItem.price.tier)

            Card(
                elevation = Dimens.dp4,
            ) {
                PriceCard(
                    params = PriceCardParams.ofFtc(context, cartItem)
                )
            }

            CheckoutMessage(text = cartItem.intent.message)

            Spacer(modifier = Modifier.height(Dimens.dp16))

            radioOptions.forEach { payMethod ->
                PayMethodOption(
                    method = payMethod,
                    selected = (payMethod == selectOption),
                    enabled = !forbidden,
                    onSelect = onOptionSelected,
                )
            }
        }

        BlockButton(
            onClick = {
                selectOption?.let{
                    onClickPay(it)
                }
            },
            enabled = (!loading && selectOption != null && !forbidden),
            text = stringResource(id = R.string.check_out)
        )
    }
}

@Composable
private fun PayMethodOption(
    method: PayMethod,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (PayMethod) -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable(
                onClick = {
                    onSelect(method)
                },
                enabled = enabled
            )
            .padding(Dimens.dp8)
            .fillMaxWidth(),
    ) {

        PaymentBrandItem(payMethod = method)

        RadioIcon(
            selected = selected,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFtcPayBody() {
    FtcPayScreen(
        cartItem = CartItemFtc(
            intent = CheckoutIntent(
                kind = IntentKind.Create,
                message = "",
            ),
            price = defaultPaywall.products[0].prices[0],
            discount = null,
            isIntro = false,
        ),
        loading = true,
        onClickPay = {}
    )
}
