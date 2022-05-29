package com.ft.ftchinese.ui.account.delete

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.BlockButton
import com.ft.ftchinese.ui.components.PasswordInput
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.components.rememberInputState
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.ValidationRule
import com.ft.ftchinese.ui.validator.Validator

@Composable
fun DeleteAccountScreen(
    loading: Boolean,
    onVerify: (String) -> Unit
) {

    val currentPwState = rememberInputState(
        rules = listOf(
            ValidationRule(
                predicate = Validator::notEmpty,
                message = "密码必填"
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
            state = currentPwState,
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        BlockButton(
            enabled = currentPwState.valid.value && !loading,
            onClick = {
                onVerify(currentPwState.field.value)
            },
            text = stringResource(id = R.string.btn_verify_password)
        )
    }
}

@Composable
fun AlertDeleteDenied(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    SimpleDialog(
        title = stringResource(id = R.string.title_delete_account_denied),
        body = stringResource(id = R.string.message_delete_account_valid_subs),
        confirmText = stringResource(id = R.string.btn_send_email),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
