package com.ft.ftchinese.ui.main.myft

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.account.AccountActivity
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.main.home.MainNavScreen
import com.ft.ftchinese.ui.settings.SettingsActivity
import com.ft.ftchinese.ui.subs.MemberActivity
import com.ft.ftchinese.ui.subs.SubsActivity
import com.ft.ftchinese.ui.util.AccountAction
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun MyFtActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    onNavigate: (MainNavScreen) -> Unit
) {
    val context = LocalContext.current
    val account by userViewModel.accountLiveData.observeAsState()
    val isLoggedIn = userViewModel.loggedInLiveData.observeAsState(false)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                // Note the account state enclosed here won't be updated.
                userViewModel.reloadAccount()
                result.data?.let {
                    when (IntentsUtil.getAccountAction(it)) {
                        AccountAction.SignedIn -> {
                            context.toast(R.string.login_success)
                        }
                        AccountAction.Deleted -> {
                            context.toast(R.string.message_account_deleted)
                        }
                        else -> {}
                    }
                }
            }
            Activity.RESULT_CANCELED -> {

            }
        }
    }

    val (alertLogout, setAlertLogout) = remember {
        mutableStateOf(false)
    }

    if (alertLogout) {
        AlertLogout(
            onDismiss = {setAlertLogout(false)},
            onConfirm = {
                userViewModel.logout()
                setAlertLogout(false)
                context.toast(R.string.action_logout)
            }
        )
    }

    MyFtScreen(
        loggedIn = isLoggedIn.value,
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
                    // Launch setting for result in case user clicked logout button.
                    SettingsActivity.start(context)
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
    ) {
        Avatar(
            loggedIn = isLoggedIn.value,
            onLogin = {
                AuthActivity.launch(
                    launcher = launcher,
                    context = context
                )
            },
            onLogout = {
                setAlertLogout(true)
            },
            displayName = account?.displayName,
            imageUrl = account?.wechat?.avatarUrl
        )
    }
}
