package com.ft.ftchinese.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.TabPages
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.components.launchWxLogin
import com.ft.ftchinese.ui.main.home.ChannelPagerScreen
import com.ft.ftchinese.ui.main.home.MainBottomBar
import com.ft.ftchinese.ui.main.home.MainNavScreen
import com.ft.ftchinese.ui.main.home.MainToolBar
import com.ft.ftchinese.ui.main.myft.MyFtActivityScreen
import com.ft.ftchinese.ui.main.myft.ReadArticleActivityScreen
import com.ft.ftchinese.ui.main.myft.StarredArticleActivityScreen
import com.ft.ftchinese.ui.main.myft.TopicsActivityScreen
import com.ft.ftchinese.ui.main.search.SearchActivityScreen
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.util.ShareUtils
import com.ft.ftchinese.viewmodel.UserViewModel
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme



@Composable
fun MainApp(
    userViewModel: UserViewModel,
) {

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    
    val currentScreen = MainNavScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    val appState = rememberMainAppState(
        scaffoldState = scaffoldState
    )

    val (wxReAuth, setWxReAuth) = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = Unit) {
        appState.trackAppOpened()
        val wxExpired = userViewModel.isWxSessionExpired()

        if (wxExpired) {
            setWxReAuth(true)
        }
    }

    if (wxReAuth) {
        SimpleDialog(
            title = stringResource(id = R.string.login_expired),
            body = stringResource(id = R.string.wx_session_expired),
            onDismiss = { setWxReAuth(false) },
            onConfirm = {
                launchWxLogin(ShareUtils.createWxApi(context))
                setWxReAuth(false)
            }
        )
    }

    OTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.primary)
                .systemBarsPadding(),
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
                        userViewModel = userViewModel,
                        scaffoldState = scaffoldState,
                        channelSources = TabPages.newsPages,
                        onTabSelected = {
                            appState.trackChannelSelected(it.title)
                        }
                    )
                }

                composable(
                    route = MainNavScreen.English.route
                ) {
                    ChannelPagerScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffoldState,
                        channelSources = TabPages.englishPages,
                        onTabSelected = {
                            appState.trackChannelSelected(it.title)
                        }
                    )
                }

                composable(
                    route = MainNavScreen.FtAcademy.route
                ) {
                    ChannelPagerScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffoldState,
                        channelSources = TabPages.ftaPages,
                        onTabSelected = {
                            appState.trackChannelSelected(it.title)
                        }
                    )
                }

                composable(
                    route = MainNavScreen.Video.route
                ) {
                    ChannelPagerScreen(
                        userViewModel = userViewModel,
                        scaffoldState = scaffoldState,
                        channelSources = TabPages.videoPages,
                        onTabSelected = {
                            appState.trackChannelSelected(it.title)
                        }
                    )
                }

                composable(
                    route = MainNavScreen.MyFt.route
                ) {
                    MyFtActivityScreen(
                        userViewModel = userViewModel,
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



