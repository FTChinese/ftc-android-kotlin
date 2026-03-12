package com.ft.ftchinese.ui.account.password

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.auth.PasswordActivity
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

    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = uiState::dismissSuccessDialog,
            title = {
                Text(text = "提示")
            },
            text = {
                Text(text = stringResource(id = R.string.prompt_saved))
            },
            confirmButton = {
                TextButton(onClick = uiState::dismissSuccessDialog) {
                    Text("确定")
                }
            }
        )
    }

    ProgressLayout(
        loading = uiState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        PasswordScreen(
            loading = uiState.progress.value,
            clearForm = uiState.clearForm,
            onFormCleared = uiState::onFormCleared,
            onSave = {
                uiState.changePassword(
                    ftcId = account.id,
                    params = it,
                )
            }
        )
    }
}
