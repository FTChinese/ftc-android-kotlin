package com.ft.ftchinese.ui.main

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.main.home.MainNavScreen
import com.ft.ftchinese.ui.main.myft.MyFtActivityScreen
import com.ft.ftchinese.ui.main.myft.ReadArticleActivityScreen
import com.ft.ftchinese.ui.main.myft.StarredArticleActivityScreen
import com.ft.ftchinese.ui.main.myft.TopicsActivityScreen
import com.ft.ftchinese.ui.theme.OTheme

class MyFtActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OTheme {
                MyFtApp {
                    finish()
                }
            }

        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, MyFtActivity::class.java))
        }
    }
}

@Composable
private fun MyFtApp(
    onExit: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()

    val currentScreen = MainNavScreen.fromRoute(
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
        scaffoldState = scaffoldState
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainNavScreen.MyFt.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = MainNavScreen.MyFt.route
            ) {
                MyFtActivityScreen(
                    onNavigate = {
                        navigate(
                            navController = navController,
                            screen = it
                        )
                    }
                )
            }

            composable(
                route = MainNavScreen.ReadArticles.route
            ) {
                ReadArticleActivityScreen(
                    scaffoldState = scaffoldState
                )
            }

            composable(
                route = MainNavScreen.StarredArticles.route
            ) {
                StarredArticleActivityScreen(
                    scaffoldState = scaffoldState
                )
            }

            composable(
                route = MainNavScreen.FollowedTopics.route
            ) {
                TopicsActivityScreen(
                    scaffoldState = scaffoldState
                )
            }

        }
    }
}

private fun navigate(
    navController: NavController,
    screen: MainNavScreen
) {
    navController.navigate(screen.route)
}
