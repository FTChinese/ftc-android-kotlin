package com.ft.ftchinese.ui.auth

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import com.ft.ftchinese.ui.auth.component.AuthScreen
import com.ft.ftchinese.ui.auth.email.EmailExists
import com.ft.ftchinese.ui.auth.email.EmailExistsActivityScreen
import com.ft.ftchinese.ui.auth.mobile.LinkEmailActivityScreen
import com.ft.ftchinese.ui.auth.mobile.MobileAuthActivityScreen
import com.ft.ftchinese.ui.auth.password.ForgotActivityScreen
import com.ft.ftchinese.ui.auth.password.ResetActivityScreen
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuthApp {
                finish()
            }
        }
    }
}

@Composable
fun AuthApp(
    onExit: () -> Unit
) {
    val scaffold = rememberScaffoldState()

    OTheme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()

        val currentScreen = AuthScreen.fromRoute(
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
                startDestination = AuthScreen.MobileLogin.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                // Mobile login screen
                composable(
                    route = AuthScreen.MobileLogin.name
                ) {
                    MobileAuthActivityScreen(
                        scaffoldState = scaffold,
                        onLinkEmail = {
                            // Go to collect email + password to link it with the mobile number user provided.
                            navigateToMobileLinkEmail(
                                navController = navController,
                                mobile = it,
                            )
                        },
                        // Go to the screen of checking email existence.
                        onEmailLogin = {
                            navigateTo(
                                navController,
                                AuthScreen.EmailExists
                            )
                        },
                        // Login success. Destroy the activity.
                        onSuccess = onExit,
                    )
                }

                composable(
                    route = "${AuthScreen.MobileLinkEmail.name}/{mobile}",
                    arguments = listOf(
                        navArgument("mobile") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val mobile = entry.arguments?.getString("mobile")
                    LinkEmailActivityScreen(
                        scaffoldState = scaffold,
                        mobile = mobile,
                        onSuccess = onExit,
                        onForgotPassword = { email ->
                            navigateToForgotPassword(
                                navController,
                                email,
                            )
                        },
                        onSignUp = {
                            navigateTo(
                                navController,
                                AuthScreen.EmailSignUp,
                            )
                        }
                    )
                }

                composable(
                    route = AuthScreen.EmailExists.name
                ) {
                    EmailExistsActivityScreen(
                        scaffoldState = scaffold,
                        onSuccess = {
                            navigateToEmailAuth(
                                navController,
                                it,
                            )
                        }
                    )
                }

                composable(
                    route = AuthScreen.EmailLogin.name
                ) {

                }

                composable(
                    route = AuthScreen.EmailSignUp.name
                ) {

                }

                composable(
                    route = "${AuthScreen.ForgotPassword.name}/{email}",
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
                    route = "${AuthScreen.ResetPassword.name}/{token}?email={email}",
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
                        onSuccess = onExit
                    )
                }
            }
        }
    }
}

private fun navigateTo(
    navController: NavController,
    screen: AuthScreen,
) {
    navController.navigate(screen.name)
}

private fun navigateToMobileLinkEmail(
    navController: NavController,
    mobile: String,
) {
    navController.navigate("${AuthScreen.MobileLinkEmail.name}/${mobile}")
}

private fun navigateToEmailAuth(
    navController: NavController,
    emailExists: EmailExists,
) {
    val screen = if (emailExists.exists) {
        AuthScreen.EmailLogin
    } else {
        AuthScreen.EmailSignUp
    }
    navController.navigate("${screen.name}/${emailExists.email}")
}


private fun navigateToForgotPassword(
    navController: NavController,
    email: String
) {
    navController.navigate("${AuthScreen.ForgotPassword.name}/${email}")
}

private fun navigateToPasswordReset(
    navController: NavController,
    bearer: PwResetBearer,
) {
    navController.navigate("${AuthScreen.ResetPassword.name}/${bearer.token}?email=${bearer.email}")
}
