package com.ft.ftchinese.ui.stripepay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.paywall.CartItemStripeV2
import com.ft.ftchinese.model.paywall.CheckoutIntent
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.ui.components.CheckoutHeader
import com.ft.ftchinese.ui.components.CheckoutMessage
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.product.PriceCard
import com.ft.ftchinese.ui.product.PriceCardParams
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun StripePayScreen(
    cartItem: CartItemStripeV2,
    loading: Boolean,
    onSelectPayment: () -> Unit,
    onClickPay: () -> Unit,
) {

    val context = LocalContext.current

    ProgressLayout(
        loading = loading,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(all = Dimens.dp8)
            ) {
                CheckoutHeader(tier = cartItem.recurring.tier)

                Card(
                    elevation = Dimens.dp4
                ) {
                    PriceCard(
                        params = PriceCardParams.ofStripe(context, cartItem)
                    )
                }

                CheckoutMessage(text = cartItem.intent.message)

                PaymentMethodRow(
                    card = "",
                    enabled = !loading,
                    onClick = onSelectPayment,
                )
                Footnote()
            }
            
            PrimaryButton(
                onClick = onClickPay,
                enabled = !loading,
                modifier = Modifier
                    .padding(Dimens.dp16)
                    .fillMaxWidth()
            ) {
                Text(
                    text = FormatHelper.stripeIntentText(
                        context,
                        cartItem.intent.kind
                    ),
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(
    card: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            top = Dimens.dp16,
        )
    ) {
        Text(
            text = stringResource(id = R.string.stripe_payment_method),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(
                bottom = Dimens.dp8
            )
        )
        Row(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                )
                .background(OColor.black5)
                .padding(
                    top = Dimens.dp16,
                    bottom = Dimens.dp16,
                    start = Dimens.dp8,
                    end = Dimens.dp8,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.subtitle1,
            )
            Image(
                painter = painterResource(id = R.drawable.ic_keyboard_arrow_right_gray_24dp),
                contentDescription = ""
            )
        }
    }
}

@Composable
private fun Footnote() {
    Text(
        text = stringResource(id = R.string.stripe_requirement),
        style = MaterialTheme.typography.body2,
        color = OColor.black60,
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewStripePayScreen() {
    StripePayScreen(
        cartItem = CartItemStripeV2(
            intent = CheckoutIntent(
                kind = IntentKind.Create,
                message = ""
            ),
            recurring = StripePrice(
                id = "",
                active = true,
                currency = "gbp",
                liveMode = true,
                nickname = "",
                periodCount = YearMonthDay(
                    years = 1,
                    months = 0,
                    days = 0,
                ),
                tier = Tier.STANDARD,
                unitAmount = 3999,
            ),
            trial = null,
        ),
        loading = false,
        onSelectPayment = {  },
        onClickPay = {}
    )
}
