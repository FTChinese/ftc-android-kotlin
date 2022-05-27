package com.ft.ftchinese.ui.auth.password

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.BlockButton
import com.ft.ftchinese.ui.components.PasswordInput
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.passwordRules

@Composable
fun ResetScreen(
    email: String,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {

        Text(
            text = stringResource(R.string.reset_password_guide, email),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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

        BlockButton(
            enabled = pwState.valid.value && repeatPwState.valid.value && !loading,
            onClick = {
                onSubmit(pwState.field.value)
            },
            text = stringResource(id = R.string.btn_reset_password)
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
