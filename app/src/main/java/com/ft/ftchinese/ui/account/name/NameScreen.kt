package com.ft.ftchinese.ui.account.name

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.BlockButton
import com.ft.ftchinese.ui.components.TextInput
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.Validator

@Composable
fun NameScreen(
    userName: String,
    loading: Boolean,
    onSave: (String) -> Unit
) {
    val userNameState = rememberInputState(
        initialValue = userName,
        rules = listOf(
            ValidationRule(
                predicate = Validator.maxLength(32),
                message = "过长",
            ),
            ValidationRule(
                predicate = { newValue ->
                    newValue != userName
                },
                message = "不能与当前用户名相同"
            )
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        TextInput(
            label = stringResource(id = R.string.label_user_name),
            state = userNameState,
            keyboardType = KeyboardType.Ascii,
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        BlockButton(
            enabled = userNameState.valid.value && !loading,
            onClick = {
                onSave(userNameState.field.value)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUpdateNameScreen() {
    NameScreen(
        userName = "Test Name",
        loading = false,
        onSave = {}
    )
}
