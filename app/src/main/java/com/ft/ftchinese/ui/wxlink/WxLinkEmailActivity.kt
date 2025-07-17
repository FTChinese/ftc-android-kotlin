package com.ft.ftchinese.ui.wxlink

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.ui.auth.password.ForgotActivityScreen
import com.ft.ftchinese.ui.auth.password.ResetActivityScreen
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.wxlink.linkauth.SignInActivityScreen
import com.ft.ftchinese.ui.wxlink.merge.MergeActivityScreen
import com.ft.ftchinese.ui.wxlink.linkauth.WxSignUpActivityScreen
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.MaterialTheme
import com.ft.ftchinese.ui.theme.OColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.SideEffect

/**
 * UI for a wx-only user to link to an email account.
 * This is called in multiple places; therefore made into a separate navigation app.
 */
class WxLinkEmailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Use dark icons for light background (OColor.wheat or paper)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        setContent {
            val navColor = OColor.wheat.toArgb()
            SideEffect {
                window.navigationBarColor = navColor
            }
            LinkApp {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }


    companion object {

        @JvmStatic
        fun newIntent(context: Context) = Intent(context, WxLinkEmailActivity::class.java)
    }
}

@Composable
fun LinkApp(
    onExit: () -> Unit
) {
    val scaffold = rememberScaffoldState()

    OTheme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()
        val currentScreen = LinkAppScreen.fromRoute(
            backstackEntry.value?.destination?.route
        )

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.primary)
                .systemBarsPadding(),
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
                startDestination = LinkAppScreen.CurrentEmail.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                // Go to sign-in screen so that wechat could
                // link an existing email account
                composable(
                    route = LinkAppScreen.CurrentEmail.name
                ) {
                    SignInActivityScreen(
                        scaffoldState = scaffold,
                        onSuccess = {
                            navigateToMerge(
                                navController
                            )
                        },
                        onForgotPassword = { email ->
                            navigateToForgotPassword(
                                navController,
                                email,
                            )
                        },
                        onSignUp = {
                            navigateToSignUp(navController)
                        }
                    )
                }

                // After email + password is verified in LinkCurrentEmail screen,
                // jump here to show user how the wx-email
                // accounts will be merged.
                composable(
                    route = LinkAppScreen.MergeWxEmail.name
                ) {
                    MergeActivityScreen(
                        scaffoldState = scaffold,
                        // Exit WxLinkEmailActivity after success
                        onSuccess = onExit,
                    )
                }

                // Go to signup screen so that wechat could
                // create a new email account and link it.
                // You don't need to go to the MergeWxEmail
                // screen since a newly created account is always
                // safe to merge, and then are merged automatically upon creation on server.
                composable(
                    route = LinkAppScreen.NewEmail.name
                ) {
                    WxSignUpActivityScreen(
                        scaffoldState = scaffold,
                        onSuccess = onExit
                    )
                }

                composable(
                    route = "${LinkAppScreen.ForgotPassword}/?email={email}",
                    arguments = listOf(
                        navArgument("email") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val email = entry.arguments?.getString("email")
                    ForgotActivityScreen(
                        email = email ?: "",
                        scaffoldState = scaffold,
                        onVerified = {
                            navigateToPasswordReset(
                                navController = navController,
                                bearer = it,
                            )
                        }
                    )
                }

                composable(
                    route = "${LinkAppScreen.ResetPassword}/{token}?email={email}",
                    arguments = listOf(
                        navArgument("token") {
                            type = NavType.StringType
                        },
                        navArgument("email") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val token = entry.arguments?.getString("token")
                    val email = entry.arguments?.getString("email")
                    ResetActivityScreen(
                        email = email ?: "",
                        token = token,
                        scaffoldState = scaffold,
                        onSuccess = {
                            navController.popBackStack(
                                route = LinkAppScreen.CurrentEmail.name,
                                inclusive = true,
                            )
                        }
                    )
                }

            }
        }
    }
}

private fun navigateToMerge(
    navController: NavController
) {
    navController.navigate(LinkAppScreen.MergeWxEmail.name)
}

private fun navigateToSignUp(
    navController: NavController,
) {
    navController.navigate(LinkAppScreen.NewEmail.name)
}

private fun navigateToForgotPassword(
    navController: NavController,
    email: String
) {
    navController.navigate("${LinkAppScreen.ForgotPassword.name}/?email=${email}")
}

private fun navigateToPasswordReset(
    navController: NavController,
    bearer: PwResetBearer,
) {
    navController.navigate("${LinkAppScreen.ResetPassword.name}/${bearer.token}?email=${bearer.email}")
}
