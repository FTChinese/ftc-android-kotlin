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
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.auth.signup.SignUpScreen
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.ScreenHeader
import com.ft.ftchinese.ui.wxlink.merge.LinkSuccessScreen
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun WxSignUpActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val unionId = accountState.value?.unionId
    if (unionId.isNullOrBlank()) {
        context.toast("Not a wechat user!")
        return
    }

    val tokenStore = remember {
        TokenManager.getInstance(context)
    }

    val linkNewState = rememberSignUpLinkState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = linkNewState.accountLoaded) {
        linkNewState.accountLoaded?.let {
            userViewModel.saveAccount(it)
        }
    }

    if (linkNewState.accountLoaded != null) {
        LinkSuccessScreen(
            onFinish = onSuccess
        )
    } else {
        ProgressLayout(
            loading = linkNewState.progress.value,
            modifier = Modifier.fillMaxSize()
        ) {
            SignUpScreen(
                email = "",
                loading = linkNewState.progress.value,
                onSubmit = {
                    linkNewState.wxSignUp(
                        unionId,
                        Credentials(
                            email = it.email,
                            password = it.password,
                            deviceToken = tokenStore.getToken()
                        )
                    )
                }
            ) {
                ScreenHeader(
                    title = "",
                    subTitle = "账号创建后自动绑定当前微信账号"
                )
            }

        }
    }
}
