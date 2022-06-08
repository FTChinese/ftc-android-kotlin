package com.ft.ftchinese.ui.auth.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.SubHeading2
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun LoginActivityScreen(
    scaffoldState: ScaffoldState,
    email: String?,
    onSuccess: (Account) -> Unit,
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

    LaunchedEffect(key1 = loginState.emailAccount) {
        loginState.emailAccount?.let {
            onSuccess(it)
        }
    }

    ProgressLayout(
        loading = loginState.progress.value
    ) {
        LoginScreen(
            email = email,
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
            
            if (!email.isNullOrBlank()) {
                SubHeading2(
                    text = stringResource(id = R.string.instruct_sign_in),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Dimens.dp8))
            }
            
        }
    }
}
