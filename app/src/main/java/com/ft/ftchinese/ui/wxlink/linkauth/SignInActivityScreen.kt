package com.ft.ftchinese.ui.wxlink.linkauth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.model.enums.LoginMethod
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.auth.login.LoginScreen
import com.ft.ftchinese.ui.auth.login.rememberLoginState
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.ScreenHeader
import com.ft.ftchinese.ui.wxlink.merge.MergerStore
import com.ft.ftchinese.ui.wxlink.merge.WxEmailMerger
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun SignInActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    onSuccess: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUp: () -> Unit,
) {
    val context = LocalContext.current

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value
    if (account == null) {
        context.toast("Not logged in")
        return
    }

    val tokenStore = remember {
        TokenManager.getInstance(context)
    }

    val loginState = rememberLoginState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = loginState.emailAccount) {
        loginState.emailAccount?.let {
            MergerStore.setMerger(
                WxEmailMerger(
                    ftc = it,
                    wx = account,
                    loginMethod = account.loginMethod ?: LoginMethod.WECHAT
                )
            )
            onSuccess()
        }
    }

    ProgressLayout(
        loading = loginState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        LoginScreen(
            email = "",
            loading = loginState.progress.value,
            onSubmit = {
                loginState.authenticate(
                    Credentials(
                        email = it.email,
                        password =  it.password,
                        deviceToken = tokenStore.getToken()
                    )
                )
            },
            onForgotPassword = onForgotPassword,
            onSignUp = onSignUp,
        ) {
            ScreenHeader(
                title = "",
                subTitle = "验证邮箱账号密码后绑定微信"
            )
        }
    }
}
