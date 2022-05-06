package com.ft.ftchinese.ui.stripepay

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R

@Composable
fun AlertAuthentication(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = stringResource(id = R.string.title_requires_action))
        },
        text = {
            Text(text = stringResource(id = R.string.stripe_requires_action))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(text = stringResource(id = R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(id = R.string.btn_cancel))
            }
        }
    )
}
