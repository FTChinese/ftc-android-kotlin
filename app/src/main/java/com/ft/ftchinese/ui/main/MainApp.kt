package com.ft.ftchinese.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.IconSearch
import com.ft.ftchinese.ui.main.home.MainBottomBar
import com.ft.ftchinese.ui.main.home.MainNavScreen
import com.ft.ftchinese.ui.search.SearchableActivity
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun MainApp() {

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    
    val currentScreen = MainNavScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    Scaffold(
        topBar = {
            if (currentScreen.showTopBar) {
                TopAppBar(
                    title = {
                        if (currentScreen.showTopBar) {
                            when (currentScreen) {
                                MainNavScreen.News -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_menu_masthead),
                                        contentDescription = "",
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                else -> {
                                    Text(text = stringResource(id = currentScreen.titleId))
                                }
                            }
                        }
                    },
                    elevation = Dimens.dp4,
                    backgroundColor = OColor.wheat,
                    actions = {
                        IconButton(
                            onClick = {
                                SearchableActivity.start(context)
                            }
                        ) {
                            IconSearch()
                        }
                    }
                )
            }

        },
        // See https://developer.android.com/jetpack/compose/navigation#bottom-nav
        bottomBar = {
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
        }
    }
}

@Composable
private fun SearchIcon(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_search_black_24dp),
            contentDescription = "Search"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    MainApp()
}
