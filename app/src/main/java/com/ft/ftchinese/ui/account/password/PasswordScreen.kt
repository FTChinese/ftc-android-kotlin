package com.ft.ftchinese.ui.account.password

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.PasswordUpdateParams
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.form.ChangePasswordForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun PasswordScreen(
    loading: Boolean,
    onSave: (PasswordUpdateParams) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {

        ChangePasswordForm(
            loading = loading,
            onSubmit = onSave
        )
    }
}

@Composable
fun AlertPasswordMismatch(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SimpleDialog(
        title = stringResource(id = R.string.forgot_password_link),
        body = stringResource(id = R.string.password_current_incorrect),
        confirmText = stringResource(id = R.string.title_forgot_password),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
