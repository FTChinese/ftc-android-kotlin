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
            Text(text = stringResource(id = R.string.title_cancel_auto_renew))
        },
        text = {
            Text(text = stringResource(id = R.string.body_cancel_auto_renew))
        }
    )
}
