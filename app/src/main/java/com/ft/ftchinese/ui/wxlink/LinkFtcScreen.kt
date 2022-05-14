package com.ft.ftchinese.ui.wxlink

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.TextInput
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.Validator

@Composable
fun LinkFtcScreen(
    loading: Boolean,
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
            state = emailState
        )

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
}


@Preview(showBackground = true)
@Composable
fun PreviewLinkFtcScreen() {
    LinkFtcScreen(
        loading = false,
        onCheckEmail = {}
    )
}
