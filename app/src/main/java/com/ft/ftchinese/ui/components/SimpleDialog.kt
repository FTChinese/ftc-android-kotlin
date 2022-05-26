package com.ft.ftchinese.ui.components

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R

@Composable
fun SimpleDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(id = R.string.btn_ok),
    dismissText: String = stringResource(id = R.string.btn_cancel)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PrimaryButton(
                onClick = onConfirm
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = dismissText)
            }
        },
        title = {
            Text(text = title)
        },
        text = {
            Text(text = body)
        }
    )
}
