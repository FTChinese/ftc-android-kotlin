package com.ft.ftchinese.ui.account

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun UpdateNameActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffold: ScaffoldState
) {
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val nameState = rememberUpdateNameState(
        scaffoldState = scaffold
    )

    if (account == null) {
        return
    }

    nameState.updated.value?.let {
        userViewModel.saveAccount(account.withBaseAccount(it))
    }

    ProgressLayout(
        loading = nameState.progress.value
    ) {
        UpdateNameScreen(
            userName = account.userName ?: "",
            loading = nameState.progress.value,
            onSave = { newName ->
                nameState.changeName(
                    ftcId = account.id,
                    name = newName
                )
            }
        )
    }
}
