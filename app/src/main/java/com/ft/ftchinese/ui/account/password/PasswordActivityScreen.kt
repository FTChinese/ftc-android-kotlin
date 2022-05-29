package com.ft.ftchinese.ui.account.password

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.auth.password.PasswordActivity
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun PasswordActivityScreen(
    userViewModel: UserViewModel,
    scaffold: ScaffoldState
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val uiState = rememberPasswordState(
        scaffoldState = scaffold
    )

    if (account == null) {
        return
    }

    if (uiState.forgotPassword) {
        AlertPasswordMismatch(
            onConfirm = {
                PasswordActivity.start(
                    context = context,
                    email = account.email,
                )
            },
            onDismiss = uiState::closeForgotPassword
        )
    }

    ProgressLayout(
        loading = uiState.progress.value
    ) {
        PasswordScreen(
            loading = uiState.progress.value,
            onSave = {
                uiState.changePassword(
                    ftcId = account.id,
                    params = it,
                )
            }
        )
    }
}
