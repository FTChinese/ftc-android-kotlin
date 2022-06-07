package com.ft.ftchinese.ui.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.PasswordUpdateParams
import com.ft.ftchinese.ui.components.BlockButton
import com.ft.ftchinese.ui.components.PasswordInput
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.passwordRules
import com.ft.ftchinese.ui.validator.requiredRule

@Composable
fun ChangePasswordForm(
    loading: Boolean,
    onSubmit: (PasswordUpdateParams) -> Unit,
) {

    val oldPwState = rememberInputState(
        rules = listOf(
            requiredRule("必须输入当前密码")
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

    val formValid = oldPwState.valid.value && pwState.valid.value && repeatPwState.valid.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            enabled = formValid && !loading,
            onClick = {
                onSubmit(
                    PasswordUpdateParams(
                        currentPassword = oldPwState.field.value,
                        newPassword = pwState.field.value
                    )
                )
            }
        )
    }
}
