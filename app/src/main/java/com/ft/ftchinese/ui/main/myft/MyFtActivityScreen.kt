package com.ft.ftchinese.ui.main.myft

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.account.AccountActivity
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.main.home.MainNavScreen
import com.ft.ftchinese.ui.settings.SettingsActivity
import com.ft.ftchinese.ui.subs.MemberActivity
import com.ft.ftchinese.ui.subs.SubsActivity
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun MyFtActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    onNavigate: (MainNavScreen) -> Unit
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val a = userViewModel.reloadAccount()
                // TODO: we need to distinguish login/logou action.
            }
            Activity.RESULT_CANCELED -> {

            }
        }
    }

    MyFtScreen(
        loggedIn = userViewModel.isLoggedIn,
        avatarUrl = account?.wechat?.avatarUrl,
        displayName = account?.displayName,
        onLogin = {
            AuthActivity.launch(
                launcher = launcher,
                context = context
            )
        },
        onClick = { row ->
            when (row) {
                MyFtRow.Account -> {
                    AccountActivity.launch(
                        launcher = launcher,
                        context = context
                    )
                }
                MyFtRow.Paywall -> {
                    SubsActivity.launch(
                        launcher = launcher,
                        context = context
                    )
                }
                MyFtRow.Settings -> {
                    SettingsActivity.launch(
                        launcher = launcher,
                        context = context
                    )
                }
                MyFtRow.MySubs -> {
                    MemberActivity.launch(
                        launcher = launcher,
                        context = context
                    )
                }
                MyFtRow.ReadHistory -> {
                    onNavigate(MainNavScreen.ReadArticles)
                }
                MyFtRow.Bookmarked -> {
                    onNavigate(MainNavScreen.StarredArticles)
                }
                MyFtRow.Topics -> {
                    onNavigate(MainNavScreen.FollowedTopics)
                }
            }
        }
    )
}
