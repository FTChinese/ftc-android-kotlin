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
import com.ft.ftchinese.ui.components.PrimaryBlockButton
import com.ft.ftchinese.ui.components.TextInput
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.ruleEmailValid

@Composable
fun EmailForm(
    initialEmail: String,
    buttonText: String = stringResource(id = R.string.btn_save),
    loading: Boolean,
    onSubmit: (String) -> Unit
) {
    val emailState = rememberInputState(
        initialValue = initialEmail,
        rules = listOf(
            ruleEmailValid,
            ValidationRule(
                predicate = {
                    it != initialEmail
                },
                message = "不能使用当前邮箱"
            )
        )
    )

    val formValid = emailState.valid.value

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TextInput(
            label = stringResource(id = R.string.label_email),
            state = emailState,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        PrimaryBlockButton(
            enabled = formValid && !loading,
            onClick = {
                onSubmit(emailState.field.value)
            },
            text = buttonText,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEmailForm() {
    EmailForm(
        initialEmail = "abc@example.org",
        loading = false,
        onSubmit = {}
    )
}
