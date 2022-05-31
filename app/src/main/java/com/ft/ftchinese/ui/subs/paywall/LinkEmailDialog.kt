package com.ft.ftchinese.ui.subs.paywall

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R

@Composable
fun LinkEmailDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.title_link_email))
        },
        text = {
            Text(text = "您当前使用了微信登录。Stripe支付要求提供邮箱，是否绑定邮箱？")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(text = stringResource(id = R.string.btn_yes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(id = R.string.btn_no))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewLinkEmailDialog() {
    LinkEmailDialog(
        onConfirm = {},
        onDismiss = {},
    )
}
