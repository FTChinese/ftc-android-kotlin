package com.ft.ftchinese.ui.stripepay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.stripesubs.StripePaymentCard
import com.ft.ftchinese.ui.components.ClickableRow
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
    ClickableRow(
        modifier = modifier
            .background(OColor.black5)
            .padding(Dimens.dp8),
        enabled = enabled,
        onClick = onClick
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
