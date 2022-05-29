package com.ft.ftchinese.ui.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.EmailAuthFormVal
import com.ft.ftchinese.ui.components.BlockButton
import com.ft.ftchinese.ui.components.PasswordInput
import com.ft.ftchinese.ui.components.TextInput
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.passwordRules
import com.ft.ftchinese.ui.validator.ruleEmailValid

@Composable
fun EmailSignUpForm(
    initialEmail: String,
    loading: Boolean,
    onSubmit: (EmailAuthFormVal) -> Unit,
    agreement: @Composable () -> Unit = {}
) {
    val emailState = rememberInputState(
        initialValue = initialEmail,
        rules = listOf(
            ruleEmailValid,
        )
    )

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

    val formValid = emailState.valid.value && pwState.valid.value && repeatPwState.valid.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        TextInput(
            label = stringResource(id = R.string.label_email),
            state = emailState,
            keyboardType = KeyboardType.Email
        )

        PasswordInput(
            label = stringResource(id = R.string.label_new_password),
            state = pwState,
        )

        PasswordInput(
            label = stringResource(id = R.string.label_confirm_password),
            state = repeatPwState,
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        agreement()
        BlockButton(
            enabled = formValid && !loading,
            onClick = {
                onSubmit(EmailAuthFormVal(
                    email = emailState.field.value,
                    password = pwState.field.value,
                ))
            },
            text = stringResource(id = R.string.btn_sign_up)
        )
    }
}
