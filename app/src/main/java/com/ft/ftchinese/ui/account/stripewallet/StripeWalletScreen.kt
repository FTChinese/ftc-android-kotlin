package com.ft.ftchinese.ui.account.stripewallet

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.stripesubs.StripePaymentCard
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.ui.components.AddBankCard
import com.ft.ftchinese.ui.components.BankCard
import com.ft.ftchinese.ui.components.PlainTextButton
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.theme.Dimens
import java.util.*

@Composable
fun StripeWalletScreen(
    loading: Boolean,
    paymentMethod: StripePaymentMethod?,
    isDefault: Boolean,
    onSetDefault: (StripePaymentMethod) -> Unit,
    onAddCard: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp8)
    ) {
        paymentMethod?.let { pm ->

            val card = pm.card

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {

                BankCard(
                    brand = card.brand.replaceFirstChar {
                        if (it.isLowerCase()) {
                            it.titlecase(Locale.getDefault())
                        } else {
                            it.toString()
                        }
                    },
                    last4 = card.last4,
                    expYear = card.expYear,
                    expMonth = card.expMonth,
                )

                if (!isDefault) {
                    PlainTextButton(
                        onClick = { onSetDefault(pm) },
                        text = stringResource(id = R.string.set_default_payment_method),
                        modifier = Modifier.align(Alignment.TopEnd),
                        enabled = !loading
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.dp8))
        }

        AddBankCard(
            enabled = !loading,
            onClick = onAddCard,
        )
    }
}

@Composable
fun AlertModifySubsPaymentMethod(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    SimpleDialog(
        title = stringResource(id = R.string.title_change_default_payment_method),
        body = stringResource(id = R.string.body_change_default_payment_method),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = stringResource(id = R.string.confirm_change_payment_method)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewStripeWalletScreen() {
    StripeWalletScreen(
        loading = false,
        paymentMethod = StripePaymentMethod(
            id = "test-id",
            customerId = "test-id",
            card = StripePaymentCard(
                brand = "visa",
                country = "",
                expMonth = 10,
                expYear = 2025,
                last4 = "4242"
            )
        ),
        isDefault = false,
        onSetDefault = {}
    ) {

    }
}
