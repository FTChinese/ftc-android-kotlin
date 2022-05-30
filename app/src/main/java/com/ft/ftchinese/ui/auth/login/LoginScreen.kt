package com.ft.ftchinese.ui.auth.login

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ft.ftchinese.model.request.EmailAuthFormVal
import com.ft.ftchinese.ui.form.EmailSignInForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun LoginScreen(
    email: String?,
    loading: Boolean,
    onSubmit: (EmailAuthFormVal) -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUp: () -> Unit,
    heading: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        heading()

        EmailSignInForm(
            initialEmail = email ?: "",
            loading = loading,
            onSubmit = onSubmit,
            onForgotPassword = onForgotPassword,
            onSignUp = onSignUp
        )
    }
}
