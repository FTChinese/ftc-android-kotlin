package com.ft.ftchinese.ui.subs.ftcpay

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.ft.ftchinese.repository.ApiMode
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.subs.product.PriceCard
import com.ft.ftchinese.ui.subs.product.PriceCardParams
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun FtcPayScreen(
    cartItem: CartItemFtc,
    loading: Boolean,
    mode: ApiMode,
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

            if (mode != ApiMode.Live) {
                Mode(mode = mode)
            }

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
                PayMethodItem(
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

data class PaymentBrandRes(
    val drawableId: Int,
    val stringId: Int,
) {
    companion object {
        val aliPay = PaymentBrandRes(
            drawableId = R.drawable.alipay,
            stringId = R.string.pay_brand_ali
        )

        val wxPay = PaymentBrandRes(
            drawableId = R.drawable.wechat_pay,
            stringId = R.string.pay_brand_wechat
        )

        val stripe = PaymentBrandRes(
            drawableId = R.drawable.stripe,
            stringId = R.string.pay_brand_stripe
        )

        @JvmStatic
        fun of(pm: PayMethod): PaymentBrandRes? {
            return when (pm) {
                PayMethod.ALIPAY -> aliPay
                PayMethod.WXPAY -> wxPay
                PayMethod.STRIPE -> stripe
                else -> null
            }
        }
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
        mode = ApiMode.Debug,
        loading = true,
        onClickPay = {}
    )
}
