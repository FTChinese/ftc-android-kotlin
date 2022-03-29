package com.ft.ftchinese.ui.stripepay

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account

@Composable
fun CreateCustomerDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {  },
        title = {
            Text(text = stringResource(id = R.string.title_create_stripe_customer))
        },
        text = {
            Text(
                text = "Stripe支付要求提供邮箱地址，是否使用当前邮箱注册(${account.email})？"
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSuccess
            ) {
                Text(text = stringResource(id = R.string.yes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(id = R.string.no))
            }
        }
    )
}
