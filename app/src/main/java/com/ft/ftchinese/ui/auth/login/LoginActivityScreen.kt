package com.ft.ftchinese.ui.auth.login

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.form.EmailSignInForm
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun LoginActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    email: String?,
    onSuccess: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUp: () -> Unit,
) {

    val context = LocalContext.current

    val tokenStore = remember {
        TokenManager.getInstance(context)
    }

    val loginState = rememberLoginState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = loginState.accountLoaded) {
        loginState.accountLoaded?.let {
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
        loading = loginState.progress.value
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.dp16)
        ) {
            EmailSignInForm(
                initialEmail = email ?: "",
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
                onSignUp = onSignUp
            )
        }
    }
}
