package com.ft.ftchinese.ui.product

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import org.threeten.bp.LocalDate

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ProductCard(
    heading: String,
    description: String,
    smallPrint: String?,
    priceContent: @Composable () -> Unit,
) {

    Card {
        Column(
            modifier = Modifier
                .padding(Dimens.dp16),
        ) {
            ProductHeading(text = heading)

            priceContent()

            Text(
                text = stringResource(id = R.string.stripe_requirement),
                style = MaterialTheme.typography.body2,
                color = OColor.black50,
            )

            Spacer(modifier = Modifier.height(Dimens.dp16))

            description
                .split("\n")
                .forEach { line ->
                    ProductDescRow(text = line)
                }

            Spacer(modifier = Modifier.height(Dimens.dp8))

            smallPrint?.let {
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
private fun ProductDescRow(text: String) {
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
    val product = defaultPaywall.products[0]
    val ftcItems = product
        .listShoppingItems(Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.ALIPAY,
        ))

    ProductCard(
        heading = product.heading,
        description = product.description,
        smallPrint = product.smallPrint,
        priceContent = {
            PriceList(
                ftcCartItems = ftcItems,
                stripeCartItems = listOf(),
                onFtcPay = { },
                onStripePay = { }
            )
        }
    )
}
