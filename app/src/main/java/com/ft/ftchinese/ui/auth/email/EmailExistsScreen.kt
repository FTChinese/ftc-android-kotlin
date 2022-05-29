package com.ft.ftchinese.ui.auth.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.form.EmailForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun EmailExistsScreen(
    loading: Boolean,
    onSubmit: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16),
    ) {
        EmailForm(
            initialEmail = "",
            buttonText = stringResource(id = R.string.btn_next),
            loading = loading,
            onSubmit = onSubmit,
        )
    }
}
