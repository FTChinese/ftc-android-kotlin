package com.ft.ftchinese.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import kotlinx.coroutines.launch

@Composable
fun MainApp() {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scaffoldState = rememberScaffoldState(
        drawerState = drawerState
    )
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    
    val currentScreen = BottomNavScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    Scaffold(
        drawerContent = {
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp),
                onClick = { scope.launch { drawerState.close() } },
                content = { Text("Close Drawer") }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    if (currentScreen == BottomNavScreen.News) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_menu_masthead), 
                            contentDescription = "",
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(text = stringResource(id = currentScreen.titleId))
                    }
                },
                navigationIcon = {
                    MenuIcon {
                        scope.launch { drawerState.open() }
                    }
                },
                elevation = Dimens.dp4,
                backgroundColor = OColor.wheat,
                actions = {
                    SearchIcon {

                    }
                }
            )
        },
        // See https://developer.android.com/jetpack/compose/navigation#bottom-nav
        bottomBar = {
            BottomNavView(
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
            startDestination = BottomNavScreen.News.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = BottomNavScreen.News.route
            ) {
                Text(text = "News")
            }
            
            composable(
                route = BottomNavScreen.English.route
            ) {
                Text(text = "English")
            }
            
            composable(
                route = BottomNavScreen.FtAcademy.route
            ) {
                Text(text = "FT Academy")
            }
            
            composable(
                route = BottomNavScreen.Video.route
            ) {
                Text(text = "Video")
            }
            
            composable(
                route = BottomNavScreen.MyFt.route
            ) {
                Text(text = "My Ft")
            }
        }
    }
}

@Composable
private fun MenuIcon(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = "Menu",
            tint = OColor.black90,
        )
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
