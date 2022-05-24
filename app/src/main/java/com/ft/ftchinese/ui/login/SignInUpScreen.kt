package com.ft.ftchinese.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.TextInput
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.Validator

sealed class EmailExists {
    object NotChecked : EmailExists()
    object NotFound : EmailExists()
    object Found : EmailExists()
}

@Composable
fun SignInUpScreen(
    loading: Boolean,
    emailExists: EmailExists,
    onCheckEmail: (String) -> Unit
) {
    val emailState = rememberInputState(
        rules = listOf(
            ValidationRule(
                predicate = Validator::isEmail,
                message = "请输入完整的邮箱"
            )
        )
    )

    Column(
        modifier = Modifier
            .padding(Dimens.dp16)
            .fillMaxSize(),
    ) {

        TextInput(
            label = stringResource(id = R.string.label_email),
            state = emailState,
            enabled = emailExists == EmailExists.NotChecked
        )

        when (emailExists) {
            is EmailExists.NotChecked -> {
                Spacer(modifier = Modifier.height(Dimens.dp16))

                PrimaryButton(
                    onClick = {
                        onCheckEmail(emailState.field.value)
                    },
                    enabled = !loading && emailState.valid.value,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.btn_next))
                }
            }
            is EmailExists.NotFound -> {



            }
            is EmailExists.Found -> {

            }
        }
    }
}

@Composable
fun SignUpForm(
    loading: Boolean,
    onSubmit: (String) -> Unit
) {

    val pwState = rememberInputState(
        rules = listOf(
            ValidationRule(
                predicate = Validator::notEmpty,
                message = "密码不能为空",
            ),
            ValidationRule(
                predicate = Validator.minLength(8),
                message = "长度不能少于8位",
            )
        )
    )

    val repeatPwState = rememberInputState(
        rules = listOf(
            ValidationRule(
                predicate = Validator::notEmpty,
                message = "确认密码不能为空"
            ),
            ValidationRule(
                predicate = Validator.minLength(8),
                message = "长度不能少于8位"
            ),
            ValidationRule(
                predicate = {
                    it != null && it == pwState.field.value
                },
                message = "两次输入的密码不同",
            )
        )
    )

    Column {
        TextInput(
            label = stringResource(id = R.string.label_password),
            state = pwState,
            enabled = !loading
        )

        TextInput(
            label = stringResource(id = R.string.label_confirm_password),
            state = repeatPwState,
            enabled = !loading
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        PrimaryButton(
            onClick = {
                onSubmit(pwState.field.value)
            },
            enabled = !loading && pwState.valid.value && repeatPwState.valid.value,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.btn_sign_up))
        }


    }
}
