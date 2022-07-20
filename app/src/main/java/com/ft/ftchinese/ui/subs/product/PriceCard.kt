package com.ft.ftchinese.ui.subs.product

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.paywall.PriceParts
import com.ft.ftchinese.ui.components.BodyText2
import com.ft.ftchinese.ui.formatter.formatMoney
import com.ft.ftchinese.ui.formatter.formatYMD
import com.ft.ftchinese.ui.formatter.joinPriceParts
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OColors

@Composable
private fun PricePayable(parts: PriceParts) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = buildAnnotatedString {
                append(parts.symbol)
                withStyle(style = SpanStyle(fontSize = 2.em)) {
                    append(formatMoney(context, parts.amount))
                }
                append(parts.separator)
                append(formatYMD(context, parts.period, parts.isRecurring))
            },
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@Composable
private fun PriceOriginal(
    description: String,
    parts: PriceParts
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = buildAnnotatedString {
                append(description)
                if (parts.crossed) {
                    withStyle(
                        style = SpanStyle(textDecoration = TextDecoration.LineThrough)
                    ) {
                        append(joinPriceParts(context, parts))
                    }
                } else {
                    append(joinPriceParts(context, parts))
                }
            },
            style = MaterialTheme.typography.subtitle2
        )
    }
}

@Composable
private fun PriceHeading(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body1
        )
    }
    Divider()
    Spacer(modifier = Modifier.height(Dimens.dp16))
}

@Composable
private fun PriceTitle(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            color = OColor.claret
        )
    }
    Spacer(modifier = Modifier.height(Dimens.dp8))
}

@Composable
private fun PriceSmallPrint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        BodyText2(
            text = text,
            color = OColors.black50Default,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PriceCard(
    params: PriceCardParams,
) {
    Column(
        modifier = Modifier.padding(Dimens.dp8),
    ) {

        PriceHeading(text = params.heading)

        params.title?.let {
            PriceTitle(text = params.title)
        }

        PricePayable(
            parts = params.payable,
        )

        params.overridden?.let {
            PriceOriginal(
                description = it.description,
                parts = it.parts,
            )
        }

        Spacer(modifier = Modifier.height(Dimens.dp16))

        PriceSmallPrint(text = params.smallPrint ?: "")

        Spacer(modifier = Modifier.height(Dimens.dp16))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPriceCard_OneOffPurchase() {
    PriceCard(
        params = PriceCardParams(
            heading = "包年",
            title = null,
            payable = PriceParts(
                symbol = "¥",
                amount = 298.00,
                period = YearMonthDay(
                    years = 1,
                ),
                isRecurring = false,
            ),
            isAutoRenew = false,
            smallPrint = "* 仅限支付宝或微信支付"
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPriceCard_OneOffDiscount() {
    PriceCard(
        params = PriceCardParams(
            heading = "包年",
            title = null,
            payable = PriceParts(
                symbol = "¥",
                amount = 258.00,
                period = YearMonthDay(
                    years = 1,
                ),
                isRecurring = false,
            ),
            overridden = OverriddenPrice(
                description = "原价",
                parts = PriceParts(
                    symbol = "¥",
                    amount = 298.00,
                    period = YearMonthDay(
                        years = 1,
                    ),
                    isRecurring = false,
                    crossed = true,
                )
            ),
            isAutoRenew = false,
            smallPrint = "* 仅限支付宝或微信支付"
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPriceCard_Stripe() {
    PriceCard(
        params = PriceCardParams(
            heading = "Auto Renewal",
            title = null,
            payable = PriceParts(
                symbol = "£",
                amount = 39.00,
                period = YearMonthDay(
                    years = 1,
                ),
                isRecurring = true,
            ),
            overridden = null,
            isAutoRenew = true,
            smallPrint = "* Limited only to Stripe"
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPriceCard_StripeTrial() {
    PriceCard(
        params = PriceCardParams(
            heading = "Auto Renewal",
            title = "New subscription trial offer",
            payable = PriceParts(
                symbol = "£",
                amount = 1.00,
                period = YearMonthDay(
                    months = 1,
                ),
                isRecurring = false,
            ),
            overridden = OverriddenPrice(
                description = "Original ",
                parts = PriceParts(
                    symbol = "£",
                    amount = 39.00,
                    period = YearMonthDay(
                        years = 1,
                    ),
                    isRecurring = true,
                )
            ),
            isAutoRenew = true,
            smallPrint = "* Limited only to Stripe"
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPriceCard_StripeCoupon() {
    PriceCard(
        params = PriceCardParams(
            heading = "Auto Renewal",
            title = "Coupon offer -£10.00",
            payable = PriceParts(
                symbol = "£",
                amount = 29.00,
                period = YearMonthDay(
                    years = 1,
                ),
                isRecurring = false,
            ),
            overridden = OverriddenPrice(
                description = "Original ",
                parts = PriceParts(
                    symbol = "£",
                    amount = 39.00,
                    period = YearMonthDay(
                        years = 1,
                    ),
                    isRecurring = true,
                )
            ),
            isAutoRenew = true,
            smallPrint = "* Limited only to Stripe"
        ),
    )
}
