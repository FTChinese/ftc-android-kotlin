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
                composable(
                    route = AuthScreen.MobileLogin.name
                ) {

                }

                composable(
                    route = AuthScreen.MobileSignUp.name
                ) {

                }

                composable(
                    route = AuthScreen.EmailExists.name
                ) {

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
