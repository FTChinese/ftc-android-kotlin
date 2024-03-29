package com.ft.ftchinese.ui.subs.product

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.ProductItem
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.components.MarkdownText
import com.ft.ftchinese.ui.components.ProductHeader
import com.ft.ftchinese.ui.components.SubHeading2
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColors
import org.threeten.bp.LocalDate

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProductCard(
    item: ProductItem,
    onFtcPay: (item: CartItemFtc) -> Unit,
    onStripePay: (item: CartItemStripe) -> Unit,
) {

    val context = LocalContext.current

    Card {
        Column(
            modifier = Modifier
                .padding(Dimens.dp16),
        ) {
            ProductHeader(text = item.content.heading)

            item.ftcItems.forEach { cartItem ->
                Card(
                    onClick = { onFtcPay(cartItem) },
                    backgroundColor = MaterialTheme.colors.primarySurface,
                    elevation = Dimens.dp4
                ) {
                    PriceCard(
                        params = PriceCardParams.ofFtc(LocalContext.current, cartItem),
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.dp16))
            }

            item.stripeItems.forEach { cartItem ->
                Card(
                    onClick = { onStripePay(cartItem) },
                    backgroundColor = MaterialTheme.colors.primarySurface,
                    elevation = Dimens.dp4,
                ) {
                    PriceCard(
                        params = PriceCardParams.ofStripe(LocalContext.current, cartItem),
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.dp16))
            }

            context
                .resources
                .getStringArray(R.array.stripe_footnotes)
                .map {
                    AsteriskRow(text = it,)
                }

            Spacer(modifier = Modifier.height(Dimens.dp16))

            item.content.description
                .split("\n")
                .forEach { line ->
                    ProductDescRow(text = line)
                }

            Spacer(modifier = Modifier.height(Dimens.dp8))

            item.content.smallPrint?.let {
//                BodyText2(
//                    text = it,
//                    color = OColors.black50Default,
//                )
                MarkdownText(
                    markdown = it,
                    color=OColors.black50Default,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.dp16))
        }
    }
}

@Composable
private fun AsteriskRow(
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "*")
        Spacer(modifier = Modifier.width(Dimens.dp4))
        SubHeading2(
            text = text,
            textAlign = TextAlign.Start,
            color = OColors.black50Default
        )
    }
}

@Composable
private fun ProductDescRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {

        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_done_24),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(Dimens.dp4))
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProductCard() {
    val product = defaultPaywall.products[1]
    val uiItem = ProductItem.newInstance(
        product = product,
        m = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.ALIPAY,
        ),
        stripeStore = mapOf()
    )

    ProductCard(
        item = uiItem,
        onFtcPay = {}
    ) {}
}
