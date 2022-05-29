package com.ft.ftchinese.ui.auth.mobile

import android.widget.Toast
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.MobileLinkParams
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun LinkEmailActivityScreen(
    userViewModel: UserViewModel = viewModel(),
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
            Toast.makeText(
                context,
                R.string.login_success,
                Toast.LENGTH_SHORT
            ).show()
            onSuccess()
        }
    }

    ProgressLayout(
        loading = authState.progress.value
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
