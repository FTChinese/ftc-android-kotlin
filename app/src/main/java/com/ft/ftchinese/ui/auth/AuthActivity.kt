package com.ft.ftchinese.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.ui.auth.email.EmailExistsActivityScreen
import com.ft.ftchinese.ui.auth.login.LoginActivityScreen
import com.ft.ftchinese.ui.auth.mobile.LinkEmailActivityScreen
import com.ft.ftchinese.ui.auth.mobile.MobileAuthActivityScreen
import com.ft.ftchinese.ui.auth.password.ForgotActivityScreen
import com.ft.ftchinese.ui.auth.password.ResetActivityScreen
import com.ft.ftchinese.ui.auth.signup.SignUpActivityScreen
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.viewmodel.UserViewModel

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuthApp(
                onLoginSuccess = {
                    // It seems the device's physical back key won't trigger setResult.
                    setResult(Activity.RESULT_OK, IntentsUtil.signedIn)
                    finish()
                },
                onExit = {
                    finish()
                }
            )
        }
    }

    companion object {

        @JvmStatic
        fun newIntent(context: Context) = Intent(context, AuthActivity::class.java)

        @JvmStatic
        fun launch(
            launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
            context: Context,
        ) {
            launcher.launch(
                Intent(context, AuthActivity::class.java)
            )
        }
    }
}

@Composable
fun AuthApp(
    onLoginSuccess: () -> Unit,
    onExit: () -> Unit
) {
    val scaffold = rememberScaffoldState()
    val userViewModel: UserViewModel = viewModel()

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
                        userViewModel = userViewModel,
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
                            navigateToEmailExists(
                                navController,
                            )
                        },
                        onFinish = onExit,
                        // Login success. Destroy the activity.
                        onSuccess = onLoginSuccess
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
                        userViewModel = userViewModel,
                        scaffoldState = scaffold,
                        mobile = mobile,
                        onSuccess = {
                            onExit()
                        },
                        onForgotPassword = { email ->
                            navigateToForgotPassword(
                                navController,
                                email,
                            )
                        }
                    ) {
                        navigateToSignUp(
                            navController,
                            "",
                        )
                    }
                }

                composable(
                    route = AuthScreen.EmailExists.name
                ) {
                    EmailExistsActivityScreen(
                        scaffoldState = scaffold,
                        onSuccess = {
                            if (it.exists) {
                                navigateToLogin(
                                    navController,
                                    it.email
                                )
                            } else {
                                navigateToSignUp(
                                    navController,
                                    it.email
                                )
                            }
                        }
                    )
                }

                composable(
                    route = "${AuthScreen.EmailLogin.name}/?email={email}",
                    arguments = listOf(
                        navArgument("email") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val email = entry.arguments?.getString("email")
                    LoginActivityScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffold,
                        email = email,
                        onSuccess = onLoginSuccess,
                        onForgotPassword = { email1 ->
                            navigateToForgotPassword(
                                navController,
                                email1,
                            )
                        },
                        onSignUp = {
                            navigateToSignUp(
                                navController,
                                "",
                            )
                        }
                    )
                }

                composable(
                    route = "${AuthScreen.EmailSignUp.name}/?email={email}",
                    arguments = listOf(
                        navArgument("email") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val email = entry.arguments?.getString("email")
                    SignUpActivityScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffold,
                        email = email,
                        onSuccess = onLoginSuccess
                    )
                }

                composable(
                    route = "${AuthScreen.ForgotPassword.name}/?email={email}",
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
                        onSuccess = {
                            navController.popBackStack(
                                route = AuthScreen.EmailLogin.name,
                                inclusive = true,
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun navigateToEmailExists(
    navController: NavController,
) {
    navController.navigate(AuthScreen.EmailExists.name)
}

private fun navigateToMobileLinkEmail(
    navController: NavController,
    mobile: String,
) {
    navController.navigate("${AuthScreen.MobileLinkEmail.name}/${mobile}")
}

private fun navigateToLogin(
    navController: NavController,
    email: String
) {
    navController.navigate("${AuthScreen.EmailLogin.name}/?email=${email}")
}

private fun navigateToSignUp(
    navController: NavController,
    email: String
) {
    navController.navigate("${AuthScreen.EmailSignUp.name}/?email=${email}")
}

private fun navigateToForgotPassword(
    navController: NavController,
    email: String
) {
    navController.navigate("${AuthScreen.ForgotPassword.name}/?email=${email}")
}

private fun navigateToPasswordReset(
    navController: NavController,
    bearer: PwResetBearer,
) {
    navController.navigate("${AuthScreen.ResetPassword.name}/${bearer.token}?email=${bearer.email}")
}
