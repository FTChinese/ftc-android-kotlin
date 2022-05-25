package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import kotlinx.coroutines.launch

/**
 * Show user's account details.
 * Show different fragments based on whether FTC account is bound to wechat account.
 * If user logged in with email account, show FtcAccountFragment;
 * If user logged in with wechat account and it is not bound to an FTC account, show WxAccountFragment;
 * If user logged in with wechat account and it is bound to an FTC account, show FtcAccountFragment.
 */
class AccountActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AccountApp(
                onExit = { finish() }
            )
        }
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
    onExit: () -> Unit
) {
    val scaffold = rememberScaffoldState()
    val scope = rememberCoroutineScope()

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
                        showSnackBar = {
                           scope.launch {
                               scaffold.snackbarHostState.showSnackbar(it)
                           }
                        },
                        onNavigateTo = { screen ->
                            navigateToScreen(
                                navController,
                                screen,
                            )
                        }
                    )
                }

                composable(
                    route = AccountAppScreen.Email.name
                ) {
                    UpdateEmailActivityScreen(
                        scaffold = scaffold,
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


