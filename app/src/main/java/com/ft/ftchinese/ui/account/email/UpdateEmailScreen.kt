package com.ft.ftchinese.ui.account.email

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.SecondaryButton
import com.ft.ftchinese.ui.form.EmailForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun UpdateEmailScreen(
    email: String,
    isVerified: Boolean,
    loading: Boolean,
    onVerify: () -> Unit,
    onSave: (String) -> Unit,
) {

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

        EmailForm(
            initialEmail = email,
            loading = loading,
            onSubmit = onSave
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
            enabled = !loading,
            text = stringResource(id = R.string.btn_request_verify)
        )
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
