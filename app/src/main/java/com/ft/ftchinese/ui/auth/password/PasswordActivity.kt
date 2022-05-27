package com.ft.ftchinese.ui.auth.password

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
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme

class PasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = intent.getStringExtra(ARG_EMAIL)

        setContent {
            PasswordApp(
                email = email,
                onExit =  {
                    finish()
                }
            )
        }
    }

    companion object {
        private const val ARG_EMAIL = "arg_email"

        fun start(context: Context?, email: String) {
            val intent = Intent(context, PasswordActivity::class.java).apply {
                putExtra(ARG_EMAIL, email)
            }

            context?.startActivity(intent)
        }
    }
}

@Composable
fun PasswordApp(
    email: String?,
    onExit: () -> Unit
) {
    val scaffold = rememberScaffoldState()

    OTheme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()

        val currentScreen = PasswordAppScreen.fromRoute(
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
                startDestination = PasswordAppScreen.Forgot.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = PasswordAppScreen.Forgot.name
                ) {
                    ForgotActivityScreen(
                        email = email,
                        scaffoldState = scaffold,
                        onVerified = {
                            navigateToReset(
                                navController = navController,
                                bearer = it,
                            )
                        }
                    )
                }

                composable(
                    route = "${PasswordAppScreen.Reset.name}/{token}?email={email}",
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
                    val email2 = entry.arguments?.getString("email")
                    ResetActivityScreen(
                        email = email2 ?: "",
                        token = token,
                        scaffoldState = scaffold,
                        onSuccess = onExit
                    )
                }
            }
        }
    }
}

private fun navigateToReset(
    navController: NavController,
    bearer: PwResetBearer,
) {
    navController.navigate("${PasswordAppScreen.Reset.name}/${bearer.token}?email=${bearer.email}")
}
