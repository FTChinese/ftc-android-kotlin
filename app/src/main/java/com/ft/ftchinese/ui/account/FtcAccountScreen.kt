package com.ft.ftchinese.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.ListItemTwoLine
import com.ft.ftchinese.ui.components.RightArrow
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun FtcAccountFragmentScreen(
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val refreshing by userViewModel.refreshingLiveData.observeAsState(false)
    val account = remember {
        userViewModel.account
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = refreshing
        ),
        onRefresh = {
            userViewModel.refreshAccount()
        }
    ) {
        account?.let {
            FtcAccountScreen(
                rows = buildAccountRows(context, it),
                onClickRow = { rowId ->

                }
            )
        }
    }
}

@Composable
fun FtcAccountScreen(
    rows: List<AccountRow>,
    onClickRow: (AccountRowType) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(
            rememberScrollState()
        )
    ) {
        rows.forEach { row ->
            AccountRow(
                primary = row.primary,
                secondary = row.secondary
            ) {
                onClickRow(row.id)
            }
        }
    }
}

@Composable
private fun AccountRow(
    primary: String,
    secondary: String,
    onClick: () -> Unit
) {
    ClickableRow(
        onClick = onClick,
        endIcon = {
            RightArrow()
        },
        modifier = Modifier
            .padding(Dimens.dp16)
    ) {
        ListItemTwoLine(
            primary = primary,
            secondary = secondary
        )
    }
}
