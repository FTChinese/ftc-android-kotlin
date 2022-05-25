package com.ft.ftchinese.ui.account

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun UpdateEmailActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffold: ScaffoldState
) {
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val updateEmailState = rememberUpdateEmailState(
        scaffoldState = scaffold
    )

    if (account == null) {
        return
    }

    updateEmailState.updated.value?.let {
        userViewModel.saveAccount(account.withBaseAccount(it))
    }

    ProgressLayout(
        loading = updateEmailState.progress.value
    ) {
        UpdateEmailScreen(
            email = account.email,
            isVerified = account.isVerified,
            loading = updateEmailState.progress.value,
            onVerify = {
                updateEmailState.requestVrfLetter(account.id)
            },
            onSave = { newEmail ->
                updateEmailState.updateEmail(
                    ftcId = account.id,
                    newEmail
                )
            }
        )
    }
}
