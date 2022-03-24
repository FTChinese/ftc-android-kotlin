package com.ft.ftchinese.ui.product

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Divider
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
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.PaywallProduct
import com.ft.ftchinese.model.paywall.StripePriceIDsOfProduct
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OTheme
import org.threeten.bp.LocalDate

@Composable
fun ProductHeading(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h5,
        )
    }
    Divider(color = OColor.teal)
    Spacer(modifier = Modifier.height(Dimens.dp16))
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ProductCard(
    product: PaywallProduct,
    stripePrices: Map<String, StripePrice>,
    membership: Membership,
) {

    val ftcItems = product.listShoppingItems(membership)

    val stripeItems = StripePriceIDsOfProduct
        .newInstance(ftcItems)
        .listShoppingItems(stripePrices, membership)

    Card {
        Column(
            modifier = Modifier
                .padding(Dimens.dp16),
        ) {
            ProductHeading(text = product.heading)

            ftcItems.forEach { cartItem ->
                PriceCard(
                    params = PriceCardParams
                        .ofFtc(LocalContext.current, cartItem),
                    onClick = {}
                )
            }

            stripeItems.forEach { cartItem ->
                PriceCard(
                    params = PriceCardParams
                        .ofStripe(LocalContext.current, cartItem),
                    onClick = {},
                )
            }

            Text(
                text = stringResource(id = R.string.stripe_requirement),
                style = MaterialTheme.typography.body2,
                color = OColor.black50,
            )

            Spacer(modifier = Modifier.height(Dimens.dp16))

            product.descWithDailyCost().split("\n").forEach { line ->
                ProductDescRow(text = line)
            }

            Spacer(modifier = Modifier.height(Dimens.dp8))

            product.smallPrint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.body2,
                    color = OColor.black50,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.dp16))
        }
    }
}

@Composable
fun ProductDescRow(text: String) {
    Row {
       Image(
           painter = painterResource(id = R.drawable.ic_done_gray_24dp),
           contentDescription = null,
       )

        Spacer(modifier = Modifier.width(Dimens.dp4))

        Text(
            text = text,
            style = MaterialTheme.typography.body1,
        )
    }
}

@Preview
@Composable
fun PreviewProductCard() {
    OTheme {
        ProductCard(
            product = defaultPaywall.products[0],
            stripePrices = mapOf(),
            membership = Membership(
                tier = Tier.STANDARD,
                cycle = Cycle.YEAR,
                expireDate = LocalDate.now().plusMonths(6),
                payMethod = PayMethod.ALIPAY,
            )
        )
    }
}
