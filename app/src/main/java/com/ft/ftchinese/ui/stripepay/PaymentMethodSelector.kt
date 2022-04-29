package com.ft.ftchinese.ui.stripepay

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
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
    Column(
        modifier = Modifier.padding(
            top = Dimens.dp8,
            bottom = Dimens.dp8
        )
    ) {
        Text(
            text = stringResource(id = R.string.stripe_payment_method),
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.dp8),
            textAlign = TextAlign.Center
        )

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
