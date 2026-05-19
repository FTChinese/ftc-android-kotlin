package com.ft.ftchinese.ui.subs.stripepay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import com.ft.ftchinese.ui.formatter.formatMoneyParts
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
    invoicePreview: StripeInvoicePreview?,
    invoicePreviewError: String?,
    subs: StripeSubs?,
    onPaymentMethod: () -> Unit,
    onRetryInvoicePreview: () -> Unit,
    onSubscribe: () -> Unit,
    onDone: () -> Unit,
) {

    val context = LocalContext.current

    val forbidden = cartItem.isForbidden
    val isApplyCoupon = cartItem.isApplyCoupon
    val requiresPaymentMethod = cartItem.requiresPaymentMethod
    val couponEnjoyed = isApplyCoupon && couponApplied != null
    val previewRequired = cartItem.intent.kind == IntentKind.Upgrade
    val invoicePreviewReady = !previewRequired || invoicePreview != null

    val enabled = !loading &&
        (!requiresPaymentMethod || paymentMethod != null) &&
        !forbidden &&
        !couponEnjoyed &&
        invoicePreviewReady

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

            if (cartItem.intent.kind == IntentKind.Upgrade) {
                Spacer(modifier = Modifier.height(Dimens.dp8))
                UpgradeInvoicePreview(
                    invoicePreview = invoicePreview,
                    invoicePreviewError = invoicePreviewError,
                    onRetry = onRetryInvoicePreview,
                )
            }

            cartItem.coupon?.let {
                Spacer(modifier = Modifier.height(Dimens.dp8))
                CouponApplicable(
                    coupon = it,
                    applied = couponApplied
                )
            }

            if (requiresPaymentMethod) {
                Spacer(modifier = Modifier.height(Dimens.dp8))
                PaymentMethodSelector(
                    card = paymentMethod?.card,
                    clickable = !loading,
                    onClick = onPaymentMethod
                )
            }

            subs?.let {
                Spacer(modifier = Modifier.height(Dimens.dp8))
                StripeSubsDetails(
                    subs = it,
                    coupon = cartItem.coupon,
                    intentKind = cartItem.intent.kind,
                    targetTier = cartItem.recurring.tier,
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

            if (requiresPaymentMethod) {
                AutoRenewAgreement()
            }
        } else {
            PrimaryBlockButton(
                onClick = onDone,
                text = stringResource(id = R.string.btn_done)
            )
        }
    }
}

@Composable
private fun UpgradeInvoicePreview(
    invoicePreview: StripeInvoicePreview?,
    invoicePreviewError: String?,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        elevation = Dimens.dp4
    ) {
        Column(
            modifier = Modifier.padding(Dimens.dp16)
        ) {
            Text(
                text = "预计本次升级扣款",
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
            )
            Spacer(modifier = Modifier.height(Dimens.dp8))
            Text(
                text = when {
                    invoicePreviewError != null -> "暂时无法计算"
                    invoicePreview != null -> formatMoneyParts(context, invoicePreview.amountDueMoney())
                    else -> "正在计算"
                },
                style = MaterialTheme.typography.h5,
                color = if (invoicePreviewError != null) {
                    MaterialTheme.colors.error
                } else {
                    MaterialTheme.colors.primary
                },
            )
            Text(
                text = invoicePreviewError
                    ?: "最终金额以 Stripe 实际扣款为准",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
            if (invoicePreviewError != null) {
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(id = R.string.btn_retry))
                }
            }
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
        invoicePreview = null,
        invoicePreviewError = null,
        loading = false,
        onPaymentMethod = {  },
        onRetryInvoicePreview = {},
        onSubscribe = {},
        onDone = {},
        mode = ApiMode.Sandbox,
    )
}
