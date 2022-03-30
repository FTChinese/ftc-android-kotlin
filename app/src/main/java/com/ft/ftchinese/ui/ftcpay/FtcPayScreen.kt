package com.ft.ftchinese.ui.ftcpay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.FetchUi
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.model.paywall.CheckoutIntent
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.ui.components.ErrorDialog
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.product.PriceCard
import com.ft.ftchinese.ui.product.PriceCardParams
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OFont
import com.ft.ftchinese.viewmodel.AuthViewModel
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.openapi.IWXAPI

data class PayMethodRes(
    val image: Int,
    val text: Int,
)

var payMethodResources = mapOf(
    PayMethod.ALIPAY to PayMethodRes(
        image = R.drawable.alipay,
        text = R.string.pay_brand_ali
    ),
    PayMethod.WXPAY to PayMethodRes(
        image = R.drawable.wechat_pay,
        text = R.string.pay_brand_wechat
    ),
)

@Composable
fun FtcPayScreen(
    authViewModel: AuthViewModel = viewModel(),
    payViewModel: FtcPayViewModel = viewModel(),
    wxApi: IWXAPI,
    priceId: String?,
    showSnackBar: (String) -> Unit,
) {
    val context = LocalContext.current
    val (loading, setLoading) = remember {
        mutableStateOf(false)
    }

    if (priceId.isNullOrBlank()) {
        showSnackBar("Missing price id")
        return
    }

    payViewModel.stringResState?.let {
        showSnackBar(context.getString(it))
    }

    payViewModel.messageState?.let {
        showSnackBar(it)
    }

    val account = authViewModel.account
    if (account == null) {
        showSnackBar("Not logged int")
        return
    }

    payViewModel.buildCart(
        priceId = priceId,
        account = account,
    )

    val uiState = payViewModel.fetchState
    when (uiState) {
        is FetchUi.Progress -> {
            setLoading(uiState.loading)
        }
        is FetchUi.ResMsg -> {
            setLoading(false)
            ErrorDialog(
                text = context.getString(uiState.strId)
            ) {
                payViewModel.clearPaymentError()
            }
        }
        is FetchUi.TextMsg -> {
            setLoading(false)
            ErrorDialog(
                text = uiState.text
            ) {
                payViewModel.clearPaymentError()
            }
        }
    }

    payViewModel.cartItemState?.let { cartItem ->
        FtcPayBody(
            cartItem = cartItem,
            loading = loading,
            onClickPay = { payMethod ->
                if (payMethod == PayMethod.WXPAY && wxApi.wxAppSupportAPI < Build.PAY_SUPPORTED_SDK_INT) {
                    showSnackBar(context.getString(R.string.wxpay_not_supported))
                    return@FtcPayBody
                }

                payViewModel.createOrder(
                    account = account,
                    cartItem = cartItem,
                    payMethod = payMethod
                )
            }
        )
    }
}

@Composable
fun FtcPayBody(
    cartItem: CartItemFtcV2,
    loading: Boolean,
    onClickPay: (PayMethod) -> Unit,
) {
    val context = LocalContext.current

    val radioOptions = listOf(PayMethod.ALIPAY, PayMethod.WXPAY)
    val (selectOption, onOptionSelected) = remember {
        mutableStateOf<PayMethod?>(null)
    }

    ProgressLayout(
        loading = loading,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .padding(all = Dimens.dp8)
            ) {



                Text(
                    text = FormatHelper.getTier(context, cartItem.price.tier),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.dp8),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h5,
                )

                Card(
                    elevation = Dimens.dp4,
                ) {
                    PriceCard(
                        params = PriceCardParams.ofFtc(context, cartItem)
                    )
                }

                if (cartItem.intent.message.isNotBlank()) {
                    Text(
                        text = cartItem.intent.message,
                        style = MaterialTheme.typography.body2,
                        color = OColor.claret,
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.dp16))

                radioOptions.forEach { payMethod ->
                    PayMethodRow(
                        method = payMethod,
                        selected = (payMethod == selectOption),
                        enabled = (cartItem.intent.kind == IntentKind.Forbidden),
                        onSelect = onOptionSelected,
                    )
                }
            }

            PrimaryButton(
                onClick = {
                    selectOption?.let{
                        onClickPay(it)
                    }
                },
                enabled = (selectOption != null && !loading),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.check_out),
                    fontSize = OFont.blockButton,
                )
            }
        }
    }
}

@Composable
private fun PayMethodRow(
    method: PayMethod,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (PayMethod) -> Unit,
) {

    val res = payMethodResources[method] ?: return
    val name = LocalContext.current.getString(res.text)

    Row(
        modifier = Modifier
            .selectable(
                selected = selected,
                onClick = {
                    onSelect(method)
                },
                enabled = enabled,
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row {
            Image(
                painter = painterResource(id = res.image),
                contentDescription = name,
            )

            Text(
                text = name,
                modifier = Modifier.padding(start = Dimens.dp16),
                fontSize = 18.sp,

            )
        }

        RadioButton(
            selected = selected,
            onClick = {
                onSelect(method)
            },
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFtcPayBody() {
    FtcPayBody(
        cartItem = CartItemFtcV2(
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
