package com.ft.ftchinese.ui.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.PasswordResetVerifier
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ruleEmailValid
import com.ft.ftchinese.ui.validator.verifierRule

@Composable
fun ForgotPasswordForm(
    email: String?,
    loading: Boolean,
    timerState: TimerState,
    onRequestCode: (String) -> Unit, // Pass user entered mobile to host.
    onSubmit: (PasswordResetVerifier) -> Unit
) {
    val emailState = rememberInputState(
        initialValue = email ?: "",
        rules = listOf(
            ruleEmailValid,
        )
    )

    val codeState = rememberInputState(
        rules = listOf(
            verifierRule(6)
        )
    )

    val formValid = emailState.valid.value && codeState.valid.value && !loading

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TextInput(
            label = stringResource(id = R.string.label_email),
            state = emailState,
            keyboardType = KeyboardType.Email,
        )

        Spacer(modifier = Modifier.height(Dimens.dp8))

        TextInput(
            label = stringResource(id = R.string.mobile_verification_code),
            state = codeState,
            keyboardType = KeyboardType.Number,
            trailingIcon = {
                OTextButton(
                    onClick = { onRequestCode(emailState.field.value) },
                    enabled = emailState.valid.value && !timerState.isRunning && !loading,
                    text = timerState.text.value
                )
            }
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))
        BlockButton(
            enabled = formValid && !loading,
            onClick = {
                onSubmit(
                    PasswordResetVerifier(
                        email = emailState.field.value,
                        code = codeState.field.value,
                    )
                )
            },
            text = stringResource(id = R.string.btn_verify)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewForgotPasswordForm() {
    ForgotPasswordForm(
        email = "abc@example.org",
        loading = false,
        timerState = rememberTimerState(),
        onRequestCode = {},
        onSubmit = {}
    )
}
