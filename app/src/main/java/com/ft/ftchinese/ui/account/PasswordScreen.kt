package com.ft.ftchinese.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.PasswordUpdateParams
import com.ft.ftchinese.ui.components.BlockButton
import com.ft.ftchinese.ui.components.PasswordInput
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.passwordRules
import com.ft.ftchinese.ui.validator.rulePasswordRequired

@Composable
fun PasswordScreen(
    loading: Boolean,
    onSave: (PasswordUpdateParams) -> Unit
) {
    val oldPwState = rememberInputState(
        rules = listOf(
            rulePasswordRequired
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {

        PasswordInput(
            label = stringResource(id = R.string.label_old_password),
            state = oldPwState,
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
            enabled = oldPwState.valid.value && pwState.valid.value && repeatPwState.valid.value && !loading,
            onClick = {
                onSave(
                    PasswordUpdateParams(
                    currentPassword = oldPwState.field.value,
                    newPassword = pwState.field.value
                ))
            }
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
