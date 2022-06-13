package com.ft.ftchinese.ui.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.PasswordInput
import com.ft.ftchinese.ui.components.PrimaryBlockButton
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.passwordRules

@Composable
fun ResetPasswordForm(
    loading: Boolean,
    onSubmit: (String) -> Unit
) {
    val pwState = rememberInputState(
        rules = passwordRules()
    )

    val repeatPwState = rememberInputState(
        rules = passwordRules(repeat = true) + listOf(
            ValidationRule(
                predicate = {
                    it != null && it == pwState.field.value
                },
                message = "两次输入的密码不同"
            )
        )
    )

    val formValid = pwState.valid.value && repeatPwState.valid.value

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        PasswordInput(
            label = stringResource(id = R.string.label_new_password),
            state = pwState,
        )

        PasswordInput(
            label = stringResource(id = R.string.label_confirm_password),
            state = repeatPwState,
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        PrimaryBlockButton(
            enabled = formValid && !loading,
            onClick = {
                onSubmit(pwState.field.value)
            },
            text = stringResource(id = R.string.btn_reset_password)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewResetPasswordForm() {
    ResetPasswordForm(loading = false, onSubmit = {})
}
