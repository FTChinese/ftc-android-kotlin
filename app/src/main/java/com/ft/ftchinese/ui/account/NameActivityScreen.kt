package com.ft.ftchinese.ui.account

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun NameActivityScreen(
    userViewModel: UserViewModel,
    scaffold: ScaffoldState
) {
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val nameState = rememberNameState(
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
        NameScreen(
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
