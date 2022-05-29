package com.ft.ftchinese.ui.auth.password

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.form.ResetPasswordForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ResetScreen(
    email: String,
    loading: Boolean,
    onSubmit: (String) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {

        Text(
            text = stringResource(R.string.guild_reset_password, email),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        ResetPasswordForm(
            loading = loading,
            onSubmit = onSubmit
        )
    }
}

@Composable
fun AlertPasswordReset(
    onConfirm: () -> Unit
) {
    SimpleDialog(
        title = "",
        body = stringResource(id = R.string.reset_password_success),
        onDismiss = {  },
        onConfirm = onConfirm,
        dismissText = null
    )
}
