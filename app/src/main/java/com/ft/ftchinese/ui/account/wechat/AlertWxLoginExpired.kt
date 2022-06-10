package com.ft.ftchinese.ui.account.wechat

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.SimpleDialog

@Composable
fun AlertWxLoginExpired(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SimpleDialog(
        title = stringResource(id = R.string.wx_relogin),
        body = stringResource(id = R.string.wx_session_expired),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
