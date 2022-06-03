package com.ft.ftchinese.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.OColor

enum class BottomNavId {
    News,
    English,
    FtAcademy,
    Video,
    MyFt;
}

private data class NavItem(
    val id: BottomNavId,
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
)

@Composable
fun BottomNavView(
    modifier: Modifier = Modifier,
    onSelect: (BottomNavId) -> Unit
) {
    val (selectedItem, setSelectedItem) = remember {
        mutableStateOf(0)
    }
    val items = listOf(
        NavItem(
            id = BottomNavId.News,
            icon = R.drawable.news_inactive,
            title = R.string.nav_news
        ),
        NavItem(
            id = BottomNavId.English,
            icon = R.drawable.english_inactive,
            title = R.string.nav_english
        ),
        NavItem(
            id = BottomNavId.FtAcademy,
            icon = R.drawable.fta_inactive,
            title = R.string.nav_ftacademy
        ),
        NavItem(
            id = BottomNavId.Video,
            icon = R.drawable.video_inactive,
            title = R.string.nav_video
        ),
        NavItem(
            id = BottomNavId.MyFt,
            icon = R.drawable.myft_inactive,
            title = R.string.nav_myft
        )
    )

    BottomNavigation(
        backgroundColor = OColor.wheat,
        modifier = modifier,
    ) {
        items.forEachIndexed { index, item ->
            BottomNavigationItem(
                icon = {
                   Icon(
                       painter = painterResource(id = item.icon), 
                       contentDescription = stringResource(id = item.title)
                   )
                },
                label = {
                    Text(text = stringResource(id = item.title))
                },
                selected = selectedItem == index,
                onClick = {
                    setSelectedItem(index)
                    onSelect(item.id)
              },
                selectedContentColor = OColor.claret,
                unselectedContentColor = OColor.black60
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    BottomNavView(
        onSelect = {}
    )
}
