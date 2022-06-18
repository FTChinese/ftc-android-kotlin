package com.ft.ftchinese.ui.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.EmailAuthFormVal
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.passwordRules
import com.ft.ftchinese.ui.validator.ruleEmailValid

@Composable
fun EmailSignUpForm(
    initialEmail: String,
    loading: Boolean,
    onSubmit: (EmailAuthFormVal) -> Unit
) {
    val context = LocalContext.current

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

    val (agreed, setAgreed) = remember {
        mutableStateOf(false)
    }

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

        ConsentTerms(
            checked = agreed,
            onCheckedChange = { setAgreed(it) }
        )

        PrimaryBlockButton(
            enabled = formValid && !loading,
            onClick = {
                if (!agreed) {
                    context.toast("您需要同意用户协议和隐私政策")
                    return@PrimaryBlockButton
                }
                onSubmit(EmailAuthFormVal(
                    email = emailState.field.value,
                    password = pwState.field.value,
                ))
            },
            text = stringResource(id = R.string.btn_sign_up)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEmailSignUpForm() {
    EmailSignUpForm(
        initialEmail = "abc@example.org",
        loading = false,
        onSubmit = {}
    )
}
