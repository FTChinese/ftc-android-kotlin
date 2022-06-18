package com.ft.ftchinese.ui.wxlink.merge

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun MergeActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    onSuccess: () -> Unit
) {

    val context = LocalContext.current
    val linkParams = MergerStore.getMerger()
    if (linkParams == null) {
        context.toast("Accounts to link not set!")
        return
    }

    val linkState = rememberMergeState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = linkState.accountLinked) {
        linkState.accountLinked?.let {
            userViewModel.saveAccount(it)
            MergerStore.clear()
        }
    }

    if (linkState.accountLinked != null) {
        LinkSuccessScreen(
            onFinish = onSuccess
        )
    } else {
        ProgressLayout(
            loading = linkState.progress.value,
            modifier = Modifier.fillMaxSize()
        ) {
            LinkScreen(
                loading = linkState.progress.value,
                params = linkParams,
                onLink = {
                    linkState.link(it)
                }
            )
        }
    }
}
