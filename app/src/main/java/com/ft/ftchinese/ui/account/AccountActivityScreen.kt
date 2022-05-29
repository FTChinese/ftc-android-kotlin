package com.ft.ftchinese.ui.account

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.ft.ftchinese.ui.account.overview.FtcAccountActivityScreen
import com.ft.ftchinese.ui.wxinfo.WxInfoActivityScreen
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun AccountActivityScreen(
    userViewModel: UserViewModel,
    scaffold: ScaffoldState,
    onNavigateTo: (AccountAppScreen) -> Unit,
) {

    val accountState = userViewModel.accountLiveData.observeAsState()

    val account = accountState.value ?: return

    // When user is logged in with Wechat, it must be a wechat-only account.
    // After rereshing, the account linking status could only have 2 case:
    // It is kept intact, so we only need to update the the ui data;
    // It is linked to an email account (possibly on other platforms). In such case wechat info is still kept, so we only show
    // the change info without switching to AccountActivity.
    // If is impossible for a wechat-only user to become
    // an email-only user.
    if (account.isWxOnly) {
        WxInfoActivityScreen(
            userViewModel = userViewModel,
            scaffold = scaffold,
        )
    } else {
        FtcAccountActivityScreen(
            userViewModel = userViewModel,
            scaffold = scaffold,
            onNavigateTo = onNavigateTo
        )
    }
}

