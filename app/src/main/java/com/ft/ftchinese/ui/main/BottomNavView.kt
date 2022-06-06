package com.ft.ftchinese.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.OColor

sealed class BottomNavScreen(
    val route: String,
    @StringRes val titleId: Int,
    @DrawableRes val iconId: Int,
) {
    object News : BottomNavScreen("news", R.string.nav_news, R.drawable.news_inactive)
    object English : BottomNavScreen("english", R.string.nav_english, R.drawable.english_inactive)
    object FtAcademy : BottomNavScreen("ftacademy", R.string.nav_ftacademy, R.drawable.fta_inactive)
    object Video : BottomNavScreen("video", R.string.nav_video, R.drawable.video_inactive)
    object MyFt: BottomNavScreen("myft", R.string.nav_myft, R.drawable.myft_inactive)

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): BottomNavScreen =
            when (route?.substringBefore("/")) {
                News.route -> News
                English.route -> English
                FtAcademy.route -> FtAcademy
                Video.route -> Video
                MyFt.route -> MyFt
                null -> News
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}

val bottomNavItems = listOf(
    BottomNavScreen.News,
    BottomNavScreen.English,
    BottomNavScreen.FtAcademy,
    BottomNavScreen.Video,
    BottomNavScreen.MyFt,
)

/**
 * See https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#BottomNavigation(androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.Dp,kotlin.Function1)
 */
@Composable
fun BottomNavView(
    selected: BottomNavScreen,
    onClick: (BottomNavScreen) -> Unit
) {

    BottomNavigation(
        backgroundColor = OColor.wheat,
    ) {
        bottomNavItems.forEach { item ->
            BottomNavigationItem(
                icon = {
                   Icon(
                       painter = painterResource(id = item.iconId),
                       contentDescription = stringResource(id = item.titleId)
                   )
                },
                label = {
                    Text(
                        text = stringResource(id = item.titleId),
                        softWrap = false,
                        fontSize = 12.sp
                    )
                },
                selected = selected == item,
                onClick = {
                    onClick(item)
                },
                selectedContentColor = OColor.claret,
                unselectedContentColor = OColor.black60,
                alwaysShowLabel = false,
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewBottomNavView() {
    BottomNavView(
        selected = BottomNavScreen.News,
        onClick = {}
    )
}
