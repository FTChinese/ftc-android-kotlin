package com.ft.ftchinese.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.ui.base.TabPages
import com.ft.ftchinese.ui.main.home.ChannelPagerScreen
import com.ft.ftchinese.ui.main.home.MainBottomBar
import com.ft.ftchinese.ui.main.home.MainNavScreen
import com.ft.ftchinese.ui.main.home.MainToolBar
import com.ft.ftchinese.ui.main.myft.MyFtActivityScreen
import com.ft.ftchinese.ui.main.myft.ReadArticleActivityScreen
import com.ft.ftchinese.ui.main.myft.StarredArticleActivityScreen
import com.ft.ftchinese.ui.main.myft.TopicsActivityScreen
import com.ft.ftchinese.ui.search.SearchActivityScreen
import com.ft.ftchinese.ui.theme.OTheme

@Composable
fun MainApp() {

    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    
    val currentScreen = MainNavScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    OTheme {
        Scaffold(
            topBar = {
                if (currentScreen.showTopBar) {
                    MainToolBar(
                        screen = currentScreen,
                        onSearch = {
                            navigateToSearch(navController)
                        },
                        onBack = {
                            navController.popBackStack()
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
                    ChannelPagerScreen(
                        scaffoldState = scaffoldState,
                        channelSource = TabPages.newsPages
                    )
                }

                composable(
                    route = MainNavScreen.English.route
                ) {
                    ChannelPagerScreen(
                        scaffoldState = scaffoldState,
                        channelSource = TabPages.englishPages
                    )
                }

                composable(
                    route = MainNavScreen.FtAcademy.route
                ) {
                    ChannelPagerScreen(
                        scaffoldState = scaffoldState,
                        channelSource = TabPages.ftaPages
                    )
                }

                composable(
                    route = MainNavScreen.Video.route
                ) {
                    ChannelPagerScreen(
                        scaffoldState = scaffoldState,
                        channelSource = TabPages.videoPages
                    )
                }

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
                    route = MainNavScreen.Search.route
                ) {
                    SearchActivityScreen(
                        scaffoldState = scaffoldState,
                        onBack = {
                            navController.popBackStack()
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

}

private fun navigateToSearch(
    navController: NavController
) {
    navController.navigate(MainNavScreen.Search.route)
}

private fun navigate(
    navController: NavController,
    screen: MainNavScreen
) {
    navController.navigate(screen.route)
}

