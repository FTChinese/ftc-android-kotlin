package com.ft.ftchinese.ui.product

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.PaywallProduct
import com.ft.ftchinese.model.paywall.defaultPaywall

@Composable
fun ProductCard(prod: PaywallProduct) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = prod.heading,
                style = MaterialTheme.typography.subtitle1,
            )
        }

        Divider()

        Spacer(modifier = Modifier.height(16.dp))

        Column() {
            prod.descWithDailyCost().split("\n").forEach { line ->
                ProductDescRow(txt = line)
            }

            prod.smallPrint?.let {
                Text(text = it)
            }
        }
    }
}

@Composable
fun ProductDescRow(txt: String) {
    Row {
       Image(
           painter = painterResource(id = R.drawable.ic_done_gray_24dp),
           contentDescription = null,
       )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = txt,
            style = MaterialTheme.typography.body2,
        )
    }
}

@Preview
@Composable
fun PreviewProductCard() {
    ProductCard(prod = defaultPaywall.products[0])
}
