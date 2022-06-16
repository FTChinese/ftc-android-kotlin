package com.ft.ftchinese.ui.auth.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.request.MobileLinkParams
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun LinkEmailActivityScreen(
    userViewModel: UserViewModel,
    scaffoldState: ScaffoldState,
    mobile: String?,
    onSuccess: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUp: () -> Unit,
) {
    val context = LocalContext.current

    val tokenStore = remember {
        TokenManager.getInstance(context)
    }

    val authState = rememberLinkEmailState(
        scaffoldState = scaffoldState
    )

    if (mobile.isNullOrBlank()) {
        authState.showSnackBar("Parameter mobile number is required!")
        return
    }

    LaunchedEffect(key1 = authState.accountLoaded) {
        authState.accountLoaded?.let {
            userViewModel.saveAccount(it)
            onSuccess()
        }
    }

    ProgressLayout(
        loading = authState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        LinkEmailScreen(
            mobile = mobile ?: "",
            loading = authState.progress.value,
            onSubmit = {
                authState.authenticate(
                    MobileLinkParams(
                        email = it.email,
                        password = it.password,
                        mobile = mobile,
                        deviceToken = tokenStore.getToken()
                    )
                )
            },
            onForgotPassword = onForgotPassword,
            onSignUp = onSignUp
        )
    }
}
