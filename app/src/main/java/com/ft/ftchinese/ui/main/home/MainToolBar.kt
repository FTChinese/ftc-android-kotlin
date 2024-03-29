package com.ft.ftchinese.ui.main.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.IconArrowBackIOS
import com.ft.ftchinese.ui.components.IconSearch
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun MainToolBar(
    screen: MainNavScreen,
    onSearch: () -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            when (screen) {
                MainNavScreen.News -> {
                    BrandMastHead()
                }
                else -> {
                    Text(text = stringResource(id = screen.titleId))
                }
            }
        },
        navigationIcon = if (!screen.showBottomBar && screen.showTopBar) {
            {
                IconButton(
                    onClick = onBack
                ) {
                    IconArrowBackIOS()
                }
            }
        } else {
            null
        },
        elevation = Dimens.zero,
        actions = {
            IconButton(
                onClick = onSearch
            ) {
                IconSearch()
            }
        }
    )
}

@Composable
fun BrandMastHead() {
    Image(
        painter = painterResource(id = if (MaterialTheme.colors.isLight) {
            R.drawable.brand_masthead_light
        } else {
            R.drawable.brand_masthead_dark
        }),
        contentDescription = "",
        contentScale = ContentScale.FillHeight,
        modifier = Modifier
            .padding(
                vertical = Dimens.dp16
            )
            .fillMaxHeight()
    )
}

@Preview
@Composable
fun PreviewMainToolbar() {
    MainToolBar(
        screen = MainNavScreen.News,
        onSearch = { /*TODO*/ }
    ) {

    }
}
