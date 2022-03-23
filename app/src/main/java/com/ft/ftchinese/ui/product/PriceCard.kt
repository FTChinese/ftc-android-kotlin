package com.ft.ftchinese.ui.product

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ft.ftchinese.model.paywall.PriceParts
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.Space

@Composable
fun PricePayable(parts: PriceParts) {
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
fun PriceOriginal(parts: PriceParts?, crossed: Boolean = true) {
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        if (parts != null) {
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
        } else {
            Text(text = "", style = MaterialTheme.typography.subtitle2)
        }
    }
}

@Composable
fun PriceHeading(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption
        )
    }
    Divider()
    Spacer(modifier = Modifier.height(Space.dp16))
}

@Composable
fun PriceTitle(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1
        )
    }
    Spacer(modifier = Modifier.height(Space.dp8))
}

@Composable
fun PriceSmallPrint(text: String) {
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
fun PriceCard(params: PriceCardParams) {
    Column(
        modifier = Modifier.padding(8.dp),
    ) {

        PriceHeading(text = params.heading)
        
        PriceTitle(text = params.title ?: "")

        PricePayable(
            parts = params.payable,
        )

        PriceOriginal(
            parts = params.original,
            crossed = !params.isAutoRenew
        )

        Spacer(modifier = Modifier.height(Space.dp16))

        PriceSmallPrint(text = params.smallPrint ?: "")
    }
}

@Preview
@Composable
fun PreviewDiscountPrice() {
    Card {
        PriceCard(
            params = PriceCardParams(
                heading = "包年",
                title = "现在续订享折扣",
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
            )
        )
    }
}
