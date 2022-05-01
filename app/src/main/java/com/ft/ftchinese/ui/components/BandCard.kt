package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun BankCard(
    brand: String,
    last4: String,
    expYear: Int,
    expMonth: Int,
) {
    Card {
        Column(
            modifier = Modifier.padding(Dimens.dp16)
        ) {
            Text(
                text = brand,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.h6,
                color = OColor.black80
            )

            Spacer(modifier = Modifier.height(Dimens.dp16))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "**** **** ****",
                    modifier = Modifier.padding(end = Dimens.dp8)
                )

                Text(
                    text = last4,
                    style = MaterialTheme.typography.body1,
                    fontSize = 24.sp
                )
            }

            Text(
                text = "${expYear}/${expMonth}",
                style = MaterialTheme.typography.body2,
                color = OColor.black60
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddBankCard(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.dp16)
        ) {

            IconAddCircle(
                tint = OColor.teal
            )

            Text(
                text = stringResource(id = R.string.add_or_select_payment_method),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(start = Dimens.dp8),
                color = OColor.teal
            )
        }
    }
    
}

@Preview(showBackground = true)
@Composable
fun PreviewBankCard() {
    BankCard(
        brand = "American Express",
        last4 = "4242",
        expYear = 2025,
        expMonth = 9
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAddBankCard() {
    AddBankCard(
        enabled = true
    ) {

    }
}
