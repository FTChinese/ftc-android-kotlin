package com.ft.ftchinese.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.Validator

@Composable
fun UpdateEmailScreen(
    email: String,
    isVerified: Boolean,
    loading: Boolean,
    onVerify: () -> Unit,
    onSave: (String) -> Unit,
) {
    val emailState = rememberInputState(
        initialValue = email,
        rules = listOf(
            ValidationRule(
                predicate = Validator::isEmail,
                message = "请输入完整的邮箱"
            ),
            ValidationRule(
                predicate = {
                    it != email
                },
                message = "不能使用当前邮箱"
            )
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        if (!isVerified) {
            RequestVerificationLetter(
                loading = loading,
                onClick = onVerify,
            )
            Spacer(modifier = Modifier.height(Dimens.dp16))
        }

        TextInput(
            label = stringResource(id = R.string.label_email),
            state = emailState,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        SaveButton(
            enabled = emailState.valid.value && !loading,
            onClick = {
                onSave(emailState.field.value)
            }
        )
    }
}

@Composable
fun RequestVerificationLetter(
    loading: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.instruct_verify_email),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        SecondaryButton(
            onClick = onClick,
            modifier = Modifier.align(Alignment.End),
            enabled = !loading
        ) {
            Text(
                text = stringResource(id = R.string.btn_request_verify)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUpdateEmailScreen() {
    UpdateEmailScreen(
        email = "abc@example.org",
        isVerified = false,
        loading = false,
        onVerify = {},
        onSave = {}
    )
}
