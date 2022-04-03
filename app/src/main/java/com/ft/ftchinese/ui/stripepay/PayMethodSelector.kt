package com.ft.ftchinese.ui.stripepay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.stripesubs.StripePaymentCard
import com.ft.ftchinese.ui.components.StripeCardRow
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun PaymentMethodSelector(
    card: StripePaymentCard?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .background(OColor.black5)
            .padding(Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (card != null) {
            StripeCardRow(
                card = card,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = "未设置",
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        top = Dimens.dp8,
                        bottom = Dimens.dp8,
                    ),
                style = MaterialTheme.typography.subtitle1,
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ic_keyboard_arrow_right_gray_24dp),
            contentDescription = ""
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPayMethodSelector() {
    PaymentMethodSelector(
        card = StripePaymentCard(
            brand = "American Express",
            country = "us",
            expYear = 2023,
            expMonth = 10,
            last4 = "1234"
        ),
        enabled = true,
        onClick = {}
    )
}
