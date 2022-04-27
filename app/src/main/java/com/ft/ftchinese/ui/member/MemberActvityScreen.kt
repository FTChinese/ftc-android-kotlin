package com.ft.ftchinese.ui.member

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun MemberActivityScreen(
    memberViewModel: MembershipViewModel,
    showSnackBar: (String) -> Unit
) {

    val isRefreshing by memberViewModel.refreshingLiveData.observeAsState(false)
    val progress by memberViewModel.progressLiveData.observeAsState(false)
    val account = remember {
        memberViewModel.account
    }

    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = isRefreshing
        ),
        onRefresh = {
            memberViewModel.refresh()
        }
    ) {
        MemberScreen(
            member = account.membership,
            loading = progress
        )
    }
}
