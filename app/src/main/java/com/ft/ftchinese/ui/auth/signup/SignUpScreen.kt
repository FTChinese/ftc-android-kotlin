package com.ft.ftchinese.ui.auth.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ft.ftchinese.model.request.EmailAuthFormVal
import com.ft.ftchinese.ui.form.EmailSignUpForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun SignUpScreen(
    email: String,
    loading: Boolean,
    onSubmit: (EmailAuthFormVal) -> Unit,
    header: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        header()

        EmailSignUpForm(
            initialEmail = email,
            loading = loading,
            onSubmit = onSubmit
        )
    }
}
