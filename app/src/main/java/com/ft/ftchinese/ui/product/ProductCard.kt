package com.ft.ftchinese.ui.product

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
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
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.theme.Space
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
    Divider()
    Spacer(modifier = Modifier.height(Space.dp8))
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ProductCard(
    product: PaywallProduct,
    membership: Membership,
) {

    val ftcItems = product.listShoppingItems(membership)

    Column {
        ProductHeading(text = product.heading)

        LazyVerticalGrid(
            cells = GridCells.Fixed(2),
            contentPadding = PaddingValues(Space.dp4),
            horizontalArrangement = Arrangement.spacedBy(Space.dp4),
            verticalArrangement = Arrangement.spacedBy(Space.dp4)
        ) {
            items(ftcItems) { cartItem ->
                Card(
                    onClick = {  },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    PriceCard(
                        params = PriceCardParams.ofFtc(LocalContext.current, cartItem)
                    )
                }

            }
        }

        Text(
            text = stringResource(id = R.string.stripe_requirement),
            style = MaterialTheme.typography.body2,
            color = OColor.black50,
        )

        Spacer(modifier = Modifier.height(Space.dp16))

        product.descWithDailyCost().split("\n").forEach { line ->
            ProductDescRow(text = line)
        }

        Spacer(modifier = Modifier.height(Space.dp8))

        product.smallPrint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.body2,
                color = OColor.black50,
            )
        }

        Spacer(modifier = Modifier.height(Space.dp16))
    }
}

@Composable
fun ProductDescRow(text: String) {
    Row {
       Image(
           painter = painterResource(id = R.drawable.ic_done_gray_24dp),
           contentDescription = null,
       )

        Spacer(modifier = Modifier.width(Space.dp4))

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
            membership = Membership(
                tier = Tier.STANDARD,
                cycle = Cycle.YEAR,
                expireDate = LocalDate.now().plusMonths(6),
                payMethod = PayMethod.ALIPAY,
            )
        )
    }
}
