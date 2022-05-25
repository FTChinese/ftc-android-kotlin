package com.ft.ftchinese.ui.account

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun UpdateEmailActivityScreen(
    userViewModel: UserViewModel,
    scaffold: ScaffoldState
) {
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val emailState = rememberUpdateEmailState(
        scaffoldState = scaffold
    )

    if (account == null) {
        return
    }

    emailState.updated.value?.let {
        userViewModel.saveAccount(account.withBaseAccount(it))
    }

    ProgressLayout(
        loading = emailState.progress.value
    ) {
        UpdateEmailScreen(
            email = account.email,
            isVerified = account.isVerified,
            loading = emailState.progress.value,
            onVerify = {
                emailState.requestVrfLetter(account.id)
            },
            onSave = { newEmail ->
                emailState.updateEmail(
                    ftcId = account.id,
                    newEmail
                )
            }
        )
    }
}
