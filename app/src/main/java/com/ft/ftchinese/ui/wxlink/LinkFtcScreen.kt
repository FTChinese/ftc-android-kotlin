package com.ft.ftchinese.ui.wxlink

import androidx.compose.foundation.layout.*
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.validator.Validator

@Composable
fun LinkFtcScreen(
    loading: Boolean,
    onCheckEmail: (String) -> Unit
) {
    var email by remember {
        mutableStateOf("")
    }

    var touched by remember {
        mutableStateOf(false)
    }

    val valid by produceState(initialValue = false, email) {
        value = Validator.isEmail(email)
    }

    Column(
        modifier = Modifier
            .padding(Dimens.dp16)
            .fillMaxSize(),
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = {
                touched = true
                email = it
            },
            label = {
                Text(text = stringResource(id = R.string.label_email))
            },
            isError = touched && !valid,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        PrimaryButton(
            onClick = {
                onCheckEmail(email)
            },
            enabled = !loading,
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
