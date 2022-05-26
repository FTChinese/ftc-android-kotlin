package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.wxinfo.WxInfoActivityScreen
import com.ft.ftchinese.viewmodel.UserViewModel

/**
 * Show user's account details.
 * Show different fragments based on whether FTC account is bound to wechat account.
 * If user logged in with email account, show FtcAccountFragment;
 * If user logged in with wechat account and it is not bound to an FTC account, show WxAccountFragment;
 * If user logged in with wechat account and it is bound to an FTC account, show FtcAccountFragment.
 */
class AccountActivity : ComponentActivity() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        setContent {
            AccountApp(
                userViewModel = userViewModel,
                onExit = { finish() },
                onLogout = {
                    userViewModel.logout()
                    Toast.makeText(
                        this,
                        R.string.message_account_deleted,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        userViewModel.reloadAccount()
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}

@Composable
fun AccountApp(
    userViewModel: UserViewModel,
    onExit: () -> Unit,
    onLogout: () -> Unit,
) {
    val scaffold = rememberScaffoldState()

    OTheme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()

        val currentScreen = AccountAppScreen.fromRoute(
            backstackEntry.value?.destination?.route
        )

        Scaffold(
            topBar = {
                Toolbar(
                    heading = stringResource(id = currentScreen.titleId),
                    onBack = {
                        val ok = navController.popBackStack()
                        if (!ok) {
                            onExit()
                        }
                    }
                )
            },
            scaffoldState = scaffold
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AccountAppScreen.Overview.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = AccountAppScreen.Overview.name
                ) {
                    AccountActivityScreen(
                        userViewModel = userViewModel,
                        scaffold = scaffold,
                    ) { screen ->
                        navigateToScreen(
                            navController,
                            screen,
                        )
                    }
                }

                composable(
                    route = AccountAppScreen.Email.name
                ) {
                    UpdateEmailActivityScreen(
                        userViewModel = userViewModel,
                        scaffold = scaffold,
                    )
                }

                composable(
                    route = AccountAppScreen.UserName.name
                ) {
                    NameActivityScreen(
                        userViewModel = userViewModel,
                        scaffold = scaffold,
                    )
                }

                composable(
                    route = AccountAppScreen.Password.name
                ) {
                    PasswordActivityScreen(
                        userViewModel = userViewModel,
                        scaffold = scaffold,
                    )
                }

                composable(
                    route = AccountAppScreen.Address.name
                ) {
                    AddressActivityScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffold,
                    )
                }

                composable(
                    route = AccountAppScreen.Wechat.name
                ) {
                    WxInfoActivityScreen(
                        userViewModel = userViewModel,
                        scaffold = scaffold,
                    )
                }

                composable(
                    route = AccountAppScreen.DeleteAccount.name
                ) {
                    DeleteAccountActivityScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffold,
                        onDeleted = onLogout,
                    )
                }
            }
        }
    }
}

private fun navigateToScreen(
    navController: NavController,
    screen: AccountAppScreen
) {
    navController.navigate(screen.name)
}


