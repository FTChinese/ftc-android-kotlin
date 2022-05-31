package com.ft.ftchinese.ui.subs.member

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R

@Composable
fun CancelStripeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text(
                    text = stringResource(id = R.string.btn_ok)
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(id = R.string.btn_cancel)
                )
            }
        },
        title = {
            Text(text = "取消订阅")
        },
        text = {
            Text(text = "该操作将关闭Stripe自动续订，当前订阅在到期前依然有效。在订阅到期前，您随时可以重新打开自动续订。")
        }
    )
}
