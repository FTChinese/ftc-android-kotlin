package com.ft.ftchinese.ui.stripepay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.CheckoutIntent
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.model.stripesubs.StripeSubs
import com.ft.ftchinese.ui.components.CheckoutHeader
import com.ft.ftchinese.ui.components.CheckoutMessage
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.product.PriceCard
import com.ft.ftchinese.ui.product.PriceCardParams
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.webpage.WebpageActivity

@Composable
fun StripePayScreen(
    cartItem: CartItemStripe,
    loading: Boolean,
    paymentMethod: StripePaymentMethod?,
    subs: StripeSubs?,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(all = Dimens.dp16),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
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

                PaymentMethodSelector(
                    card = paymentMethod?.card,
                    clickable = !loading,
                    onClick = onPaymentMethod
                )

                Footnote()
                
                subs?.let {
                    StripeSubsDetails(subs = it)
                }
            }

            if (subs == null) {
                AutoRenewAgreement()

                PrimaryButton(
                    onClick = onSubscribe,
                    enabled = (!loading && paymentMethod != null && !forbidden),
                    modifier = Modifier
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
                    Text(text = stringResource(id = R.string.btn_done))
                }
            }
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

@Composable
private fun AutoRenewAgreement() {
    val context = LocalContext.current
    val page = legalPages[2]

    val annotatedString = buildAnnotatedString {
        append("点击订阅即同意FT中文网")

        pushStringAnnotation(
            "autoRenew",
            annotation = page.url
        )
        withStyle(
            style = SpanStyle(
                color = OColor.claret
            )
        ) {
            append("《${page.title}》")
        }
        pop()

        append("相关内容")
    }

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "autoRenew",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                val url = it.item
                WebpageActivity.start(
                    context = context,
                    meta = WebpageMeta(
                        title = "",
                        url = url,
                        showMenu = true
                    )
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
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
