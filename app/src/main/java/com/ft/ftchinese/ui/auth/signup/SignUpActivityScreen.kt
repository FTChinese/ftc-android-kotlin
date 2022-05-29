package com.ft.ftchinese.ui.auth.signup

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.form.EmailSignUpForm
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun SignUpActivityScreen(
    userViewModel: UserViewModel = viewModel(),
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

    val (agreed, setAgreed) = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = signUpState.accountLoaded) {
        signUpState.accountLoaded?.let {
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
        loading = signUpState.progress.value
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.dp16)
        ) {
            EmailSignUpForm(
                initialEmail = email ?: "",
                loading = signUpState.progress.value,
                onSubmit = {
                    if (!agreed) {
                        signUpState.showSnackBar("您需要同意用户协议和隐私政策")
                    }
                    signUpState.singUp(
                        Credentials(
                            email = it.email,
                            password = it.password,
                            deviceToken = tokenStore.getToken()
                        )
                    )
                }
            ) {
                ConsentTerms(
                    selected = agreed,
                    onSelect = { setAgreed(true) }
                )
            }
        }
    }
}
