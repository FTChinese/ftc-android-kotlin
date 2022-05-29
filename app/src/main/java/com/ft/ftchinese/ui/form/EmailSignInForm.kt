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
import com.ft.ftchinese.ui.validator.passwordRules
import com.ft.ftchinese.ui.validator.ruleEmailValid

@Composable
fun EmailSignInForm(
    loading: Boolean,
    onSubmit: (EmailAuthFormVal) -> Unit
) {
    val emailState = rememberInputState(
        rules = listOf(
            ruleEmailValid,
        )
    )

    val pwState = rememberInputState(
        rules = passwordRules()
    )

    val formValid = emailState.valid.value && pwState.valid.value

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TextInput(
            label = stringResource(id = R.string.label_email),
            state = emailState,
            keyboardType = KeyboardType.Email
        )

        PasswordInput(
            label = stringResource(id = R.string.label_email),
            state = pwState,
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        BlockButton(
            enabled = formValid && !loading,
            onClick = {
                onSubmit(EmailAuthFormVal(
                    email = emailState.field.value,
                    password = pwState.field.value,
                ))
            },
            text = stringResource(id = R.string.btn_login)
        )
    }
}
