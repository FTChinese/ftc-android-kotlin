package com.ft.ftchinese.ui.account.name

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
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
        loading = nameState.progress.value,
        modifier = Modifier.fillMaxSize()
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
