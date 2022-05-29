package com.ft.ftchinese.ui.auth.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.MobileFormValue
import com.ft.ftchinese.ui.auth.component.LoginAlternatives
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.form.MobileForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun MobileAuthScreen(
    loading: Boolean,
    timerState: TimerState,
    onRequestCode: (String) -> Unit,
    onSubmit: (MobileFormValue) -> Unit,
    alternative: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        MobileForm(
            defaultMobile = "",
            loading = loading,
            timerState = timerState,
            onRequestCode = onRequestCode,
            onSave = onSubmit
        )

        Spacer(modifier = Modifier.height(Dimens.dp32))

        Text(
            text = "其他登录方式",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        alternative()
    }
}

@Composable
fun AlertMobileNotSet(
    onLinkEmail: () -> Unit, // If clicked link email, go to a new screen to enter email + password.
    onSignUp: () -> Unit, // If clicked signup, dismiss this dialog and create account.
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            PrimaryButton(
                onClick = onLinkEmail
            ) {
                Text(text = stringResource(id = R.string.mobile_login_dialog_positive_buton))
            }
        },
        dismissButton = {
            SecondaryButton(
                onClick = onSignUp
            ) {
                Text(text = stringResource(id = R.string.mobile_login_dialog_negative_button))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.mobile_login_dialog_title))
        },
        text = {
            Text(text = stringResource(id = R.string.mobile_login_dialog_message))
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMobileAuthScreen() {
    MobileAuthScreen(
        loading = false,
        timerState = rememberTimerState(),
        onRequestCode = {},
        onSubmit = {}
    ) {
        LoginAlternatives(
            onClickEmail = { /*TODO*/ }
        ) {

        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAlertMobileNotSet() {
    AlertMobileNotSet(
        onLinkEmail = { }
    ) {

    }
}
