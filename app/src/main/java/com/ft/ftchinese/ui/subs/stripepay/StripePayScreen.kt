package com.ft.ftchinese.ui.subs.stripepay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.ApiMode
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.CheckoutIntent
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.stripesubs.*
import com.ft.ftchinese.ui.components.CheckoutHeader
import com.ft.ftchinese.ui.components.CheckoutMessage
import com.ft.ftchinese.ui.components.Mode
import com.ft.ftchinese.ui.components.PrimaryBlockButton
import com.ft.ftchinese.ui.form.AutoRenewAgreement
import com.ft.ftchinese.ui.formatter.formatStripeSubsBtn
import com.ft.ftchinese.ui.subs.product.PriceCard
import com.ft.ftchinese.ui.subs.product.PriceCardParams
import com.ft.ftchinese.ui.theme.Dimens
import org.threeten.bp.ZonedDateTime

@Composable
fun StripePayScreen(
    cartItem: CartItemStripe,
    loading: Boolean,
    mode: ApiMode,
    paymentMethod: StripePaymentMethod?,
    couponApplied: CouponApplied?,
    subs: StripeSubs?,
    onPaymentMethod: () -> Unit,
    onSubscribe: () -> Unit,
    onDone: () -> Unit,
) {

    val context = LocalContext.current

    val forbidden = cartItem.isForbidden
    val isApplyCoupon = cartItem.isApplyCoupon
    val couponEnjoyed = isApplyCoupon && couponApplied != null

    val enabled = !loading && (paymentMethod != null) && !forbidden && !couponEnjoyed

    Column(
        modifier = Modifier
            .padding(all = Dimens.dp16)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (mode != ApiMode.Live) {
                Mode(mode = mode)
            }

            CheckoutHeader(tier = cartItem.recurring.tier)

            Card(
                elevation = Dimens.dp4
            ) {
                PriceCard(
                    params = PriceCardParams.ofStripe(context, cartItem)
                )
            }

            CheckoutMessage(text = cartItem.intent.message)

            cartItem.coupon?.let {
                Spacer(modifier = Modifier.height(Dimens.dp8))
                CouponApplicable(
                    coupon = it,
                    applied = couponApplied
                )
            }

            Spacer(modifier = Modifier.height(Dimens.dp8))
            PaymentMethodSelector(
                card = paymentMethod?.card,
                clickable = !loading,
                onClick = onPaymentMethod
            )

            subs?.let {
                Spacer(modifier = Modifier.height(Dimens.dp8))
                StripeSubsDetails(
                    subs = it,
                    coupon = if (isApplyCoupon) cartItem.coupon else null
                )
            }

            Spacer(modifier = Modifier.height(Dimens.dp16))
        }

        if (subs == null) {
            PrimaryBlockButton(
                onClick = onSubscribe,
                enabled = enabled,
                text = formatStripeSubsBtn(
                    context,
                    cartItem.intent.kind
                ),
            )

            AutoRenewAgreement()
        } else {
            PrimaryBlockButton(
                onClick = onDone,
                text = stringResource(id = R.string.btn_done)
            )
        }
    }
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
            coupon = StripeCoupon(
                id = "coupon-id",
                amountOff = 100,
                currency = "gbp",
                redeemBy = 0,
                priceId = "attached-price-id",
                startUtc = ZonedDateTime.now(),
                endUtc = ZonedDateTime.now().plusDays(7)
            )
        ),
        paymentMethod = null,
        subs = null,
        couponApplied = null,
        loading = false,
        onPaymentMethod = {  },
        onSubscribe = {},
        onDone = {},
        mode = ApiMode.Sandbox,
    )
}
