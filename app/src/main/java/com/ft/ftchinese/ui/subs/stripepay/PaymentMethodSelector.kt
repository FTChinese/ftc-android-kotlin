package com.ft.ftchinese.ui.subs.stripepay

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.stripesubs.StripePaymentCard
import com.ft.ftchinese.ui.components.AddBankCard
import com.ft.ftchinese.ui.components.BankCard
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun PaymentMethodSelector(
    card: StripePaymentCard?,
    clickable: Boolean,
    onClick: () -> Unit,
) {
    Column {

        if (card != null) {
            BankCard(
                brand = card.brand,
                last4 = card.last4,
                expYear = card.expYear,
                expMonth = card.expMonth,
            )

            Spacer(modifier = Modifier.height(Dimens.dp8))
        }

        AddBankCard(
            enabled = clickable,
            onClick = onClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPayMethodSelector() {
    PaymentMethodSelector(
        card = StripePaymentCard(
            brand = "Visa",
            last4 = "4242",
            country = "US",
            expMonth = 10,
            expYear = 2025
        ),
        clickable = true
    ) {

    }
}
