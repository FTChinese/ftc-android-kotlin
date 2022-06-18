package com.ft.ftchinese.ui.account.unlinkwx

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun UnlinkActivityScreen(
    userViewModel: UserViewModel,
    scaffoldState: ScaffoldState,
    onSuccess: () -> Unit
) {

    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value ?: return

    val unlinkState = rememberUnlinkState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = unlinkState.accountUnlinked) {
        unlinkState.accountUnlinked?.let {
            userViewModel.saveAccount(it)
            context.toast(R.string.refresh_success)
            onSuccess()
        }
    }

    ProgressLayout(
        loading = unlinkState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        UnlinkScreen(
            account = account,
            loading = unlinkState.progress.value,
            onUnlink = {

                unlinkState.unlink(
                    account = account,
                    anchor = it,
                )
            }
        )
    }
}
