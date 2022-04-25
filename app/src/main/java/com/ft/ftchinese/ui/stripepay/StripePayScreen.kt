package com.ft.ftchinese.ui.stripepay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.CheckoutIntent
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.stripesubs.StripePaymentCard
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.model.stripesubs.Subscription
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
    cartItem: CartItemStripe,
    loading: Boolean,
    paymentMethod: StripePaymentMethod?,
    subs: Subscription?,
    onPaymentMethod: () -> Unit,
    onSubscribe: () -> Unit,
    onDone: () -> Unit,
) {

    val context = LocalContext.current

    val forbidden = cartItem.intent.kind == IntentKind.Forbidden

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
                    .verticalScroll(rememberScrollState())
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

                PaymentMethodBlock(
                    card = paymentMethod?.card,
                    clickable = !loading,
                    onClick = onPaymentMethod
                )

                Footnote()
                
                subs?.let {
                    SubsDetails(subs = it)
                }
            }

            if (subs == null) {
                PrimaryButton(
                    onClick = onSubscribe,
                    enabled = (!loading && paymentMethod != null && !forbidden),
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
            } else {
                PrimaryButton(
                    onClick = onDone,
                    modifier = Modifier
                        .padding(Dimens.dp16)
                        .fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = R.string.action_done))
                }
            }
        }
    }
}
@Composable
private fun PaymentMethodBlock(
    card: StripePaymentCard?,
    clickable: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            top = Dimens.dp8,
            bottom = Dimens.dp8
        )
    ) {
        Text(
            text = stringResource(id = R.string.stripe_payment_method),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(Dimens.dp8)
        )
        PaymentMethodSelector(
            card = card,
            enabled = clickable,
            onClick = onClick,
        )
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
        cartItem = CartItemStripe(
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
            coupon = null
        ),
        paymentMethod = null,
        subs = null,
        loading = false,
        onPaymentMethod = {  },
        onSubscribe = {},
        onDone = {},
    )
}
