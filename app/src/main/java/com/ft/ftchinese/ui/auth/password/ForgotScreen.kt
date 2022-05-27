package com.ft.ftchinese.ui.auth.password

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.MobileFormParams
import com.ft.ftchinese.model.request.PasswordResetVerifier
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.Validator

@Composable
fun ForgotScreen(
    email: String?,
    loading: Boolean,
    timerState: TimerState,
    onRequestCode: (String) -> Unit, // Pass user entered mobile to host.
    onSubmit: (PasswordResetVerifier) -> Unit
) {

    val emailState = rememberInputState(
        initialValue = email ?: "",
        rules = listOf(
            ValidationRule(
                predicate = Validator::isEmail,
                message = "请输入正确的邮箱"
            ),
        )
    )

    val codeState = rememberInputState(
        rules = listOf(
            ValidationRule(
                predicate = Validator.minLength(6),
                message = "请输入验证码"
            )
        )
    )

    val formValid = emailState.valid.value && codeState.valid.value && !loading

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
                TextButton(
                    onClick = {
                        onRequestCode(emailState.field.value)
                    },
                    enabled = emailState.valid.value && !timerState.isRunning && !loading,
                ) {
                    Text(text = timerState.text.value)
                }
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
