package com.ft.ftchinese.ui.auth.password

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.PasswordResetVerifier
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.components.TimerState
import com.ft.ftchinese.ui.components.rememberTimerState
import com.ft.ftchinese.ui.form.ForgotPasswordForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ForgotScreen(
    email: String?,
    loading: Boolean,
    timerState: TimerState,
    onRequestCode: (String) -> Unit, // Pass user entered mobile to host.
    onSubmit: (PasswordResetVerifier) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        Text(
            text = stringResource(id = R.string.forgot_password_guide),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.body1
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))


        ForgotPasswordForm(
            email = email,
            loading = loading,
            timerState = timerState,
            onRequestCode = onRequestCode,
            onSubmit = onSubmit
        )
    }
}

@Composable
fun AlertLetterSent(
    onConfirm: () -> Unit
) {
    SimpleDialog(
        title = "",
        body = stringResource(id = R.string.forgot_password_letter_sent),
        onDismiss = { },
        onConfirm = onConfirm,
        dismissText = null
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewForgotScreen() {
    ForgotScreen(
        email = "abc@example.org",
        loading = false,
        timerState = rememberTimerState(),
        onRequestCode = {},
        onSubmit = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAlertLetterSent() {
    AlertLetterSent {

    }
}
