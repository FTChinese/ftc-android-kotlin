package com.ft.ftchinese.ui.account.stripewallet

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.ui.components.AddBankCard
import com.ft.ftchinese.ui.components.BankCard
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.theme.Dimens

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
        paymentMethod?.card?.let { card ->

//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth(),
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    TextButton(
//                        onClick = {
//                            onSetDefault(paymentMethod)
//                        },
//                        enabled = (!isDefault && !loading),
//                    ) {
//                        Text(
//                            text = "设为默认",
//                        )
//                    }
//                }

            BankCard(
                brand = card.brand,
                last4 = card.last4,
                expYear = card.expYear,
                expMonth = card.expMonth,
            )

            Spacer(modifier = Modifier.height(Dimens.dp8))
        }

        AddBankCard(
            enabled = !loading,
            onClick = onAddCard,
        )
    }
}
