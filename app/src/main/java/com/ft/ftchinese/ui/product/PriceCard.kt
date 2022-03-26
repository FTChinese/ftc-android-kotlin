package com.ft.ftchinese.ui.product

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import com.ft.ftchinese.model.paywall.PriceParts
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
private fun PricePayable(parts: PriceParts) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = buildAnnotatedString {
                append(parts.symbol)
                withStyle(style = SpanStyle(fontSize = 2.em)) {
                    append(parts.amount)
                }
                append(parts.cycle)
            },
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@Composable
private fun PriceOriginal(parts: PriceParts, crossed: Boolean = true) {
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = buildAnnotatedString {
                append(parts.notes)
                if (crossed) {
                    withStyle(
                        style = SpanStyle(textDecoration = TextDecoration.LineThrough)
                    ) {
                        append(parts.string())
                    }
                } else {
                    append(parts.string())
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
        Text(
            text = text,
            color = OColor.black50,
            style = MaterialTheme.typography.body2
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

        params.original?.let {
            PriceOriginal(
                parts = params.original,
                crossed = !params.isAutoRenew
            )
        }

        Spacer(modifier = Modifier.height(Dimens.dp16))

        PriceSmallPrint(text = params.smallPrint ?: "")

        Spacer(modifier = Modifier.height(Dimens.dp16))
    }
}

@Preview
@Composable
fun PreviewDiscountPrice() {
    PriceCard(
        params = PriceCardParams(
            heading = "包年",
            title = null,
            payable = PriceParts(
                symbol = "¥",
                amount = "258.00",
                cycle = "/1年"
            ),
            original = PriceParts(
                symbol = "¥",
                amount = "298.00",
                cycle = "/1年",
                notes = "原价",
            ),
            isAutoRenew = false,
            smallPrint = "* 仅限支付宝或微信支付"
        ),
    )
}
