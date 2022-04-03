package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.stripesubs.StripePaymentCard
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun StripeCardRow(
    card: StripePaymentCard,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = card.brand,
            style = MaterialTheme.typography.body1,
            color = OColor.black90,
            modifier = Modifier.padding(
                bottom = Dimens.dp8
            )
        )
        Row {
            Text(
                text = "**** ${card.last4}",
                modifier = Modifier.padding(
                    end = Dimens.dp16
                )
            )

            Text(
                text = "${card.expYear}/${card.expMonth}",
                style = MaterialTheme.typography.body2,
                color = OColor.black60
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStripeCardRow() {
    StripeCardRow(
        card = StripePaymentCard(
            brand = "American Express",
            country = "us",
            expYear = 2023,
            expMonth = 10,
            last4 = "1234"
        )
    )
}
