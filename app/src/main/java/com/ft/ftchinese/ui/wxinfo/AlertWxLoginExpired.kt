package com.ft.ftchinese.ui.wxinfo

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R

@Composable
fun AlertWxLoginExpired(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text(text = stringResource(id = R.string.btn_ok))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(text = stringResource(id = R.string.btn_cancel))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.wx_relogin))
        },
        text = {
            Text(text = stringResource(id = R.string.wx_session_expired))
        }
    )
}
