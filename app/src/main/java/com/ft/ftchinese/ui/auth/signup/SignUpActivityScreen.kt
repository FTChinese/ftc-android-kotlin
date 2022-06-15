package com.ft.ftchinese.ui.auth.signup

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
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.SubHeading2
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun SignUpActivityScreen(
    userViewModel: UserViewModel,
    scaffoldState: ScaffoldState,
    email: String?,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current

    val tokenStore = remember {
        TokenManager.getInstance(context)
    }

    val signUpState = rememberSignUpState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = signUpState.accountLoaded) {
        signUpState.accountLoaded?.let {
            userViewModel.saveAccount(it)
            onSuccess()
        }
    }

    ProgressLayout(
        loading = signUpState.progress.value
    ) {
        SignUpScreen(
            email = email ?: "",
            loading = signUpState.progress.value,
            onSubmit = {
                signUpState.signUp(
                    Credentials(
                        email = it.email,
                        password = it.password,
                        deviceToken = tokenStore.getToken()
                    )
                )
            }
        ) {
            if (!email.isNullOrBlank()) {
                SubHeading2(
                    text = stringResource(id = R.string.instruct_sign_up),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.dp8))
            }
        }

    }
}
