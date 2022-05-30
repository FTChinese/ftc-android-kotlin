package com.ft.ftchinese.ui.auth.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ft.ftchinese.model.request.EmailAuthFormVal
import com.ft.ftchinese.ui.components.ScreenHeader
import com.ft.ftchinese.ui.form.EmailSignInForm
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun LinkEmailScreen(
    mobile: String,
    loading: Boolean,
    onSubmit: (EmailAuthFormVal) -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUp: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {

        ScreenHeader(
            title = "关联已有邮箱账号",
            subTitle = "绑定邮箱后下次可以直接使用手机号${mobile}登录该邮箱账号"
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        EmailSignInForm(
            initialEmail = "",
            loading = loading,
            onSubmit = onSubmit,
            onForgotPassword = onForgotPassword,
            onSignUp = onSignUp,
        )
    }
}
