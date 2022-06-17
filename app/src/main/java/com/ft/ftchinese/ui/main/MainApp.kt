package com.ft.ftchinese.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.ui.main.home.MainBottomBar
import com.ft.ftchinese.ui.main.home.MainNavScreen
import com.ft.ftchinese.ui.main.home.MainToolBar
import com.ft.ftchinese.ui.search.SearchActivityScreen

@Composable
fun MainApp() {

    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    
    val currentScreen = MainNavScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    Scaffold(
        topBar = {
            if (currentScreen.showTopBar) {
                MainToolBar(
                    screen = currentScreen,
                    onSearch = {
                        navigateToSearch(navController)
                    }
                )
            }

        },
        // See https://developer.android.com/jetpack/compose/navigation#bottom-nav
        bottomBar = {
            if (currentScreen.showBottomBar) {
                MainBottomBar(
                    onClick = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    selected = currentScreen
                )
            }
        },
        scaffoldState = scaffoldState
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainNavScreen.News.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = MainNavScreen.News.route
            ) {
                Text(text = "News")
            }
            
            composable(
                route = MainNavScreen.English.route
            ) {
                Text(text = "English")
            }
            
            composable(
                route = MainNavScreen.FtAcademy.route
            ) {
                Text(text = "FT Academy")
            }
            
            composable(
                route = MainNavScreen.Video.route
            ) {
                Text(text = "Video")
            }
            
            composable(
                route = MainNavScreen.MyFt.route
            ) {
                Text(text = "My Ft")
            }

            composable(
                route = MainNavScreen.Search.route
            ) {
                SearchActivityScreen(
                    scaffoldState = scaffoldState,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private fun navigateToSearch(
    navController: NavController
) {
    navController.navigate(MainNavScreen.Search.route)
}
