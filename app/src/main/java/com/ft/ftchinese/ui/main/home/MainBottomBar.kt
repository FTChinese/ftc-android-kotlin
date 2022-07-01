package com.ft.ftchinese.ui.main.home

import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ft.ftchinese.ui.components.BottomNav
import com.ft.ftchinese.ui.components.BottomNavItem
import com.ft.ftchinese.ui.theme.OColor

val bottomNavItems = listOf(
    MainNavScreen.News,
    MainNavScreen.English,
    MainNavScreen.FtAcademy,
    MainNavScreen.Video,
    MainNavScreen.MyFt,
)

/**
 * See https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#BottomNavigation(androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.Dp,kotlin.Function1)
 */
@Composable
fun MainBottomBar(
    selected: MainNavScreen,
    onClick: (MainNavScreen) -> Unit
) {

    BottomNav {
        bottomNavItems.forEach { item ->
            BottomNavItem(
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
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewBottomNavView() {
    MainBottomBar(
        selected = MainNavScreen.News,
        onClick = {}
    )
}
