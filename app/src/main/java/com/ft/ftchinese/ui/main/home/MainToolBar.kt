package com.ft.ftchinese.ui.main.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.IconArrowBackIOS
import com.ft.ftchinese.ui.components.IconChatBot
import com.ft.ftchinese.ui.components.IconSearch
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun MainToolBar(
    screen: MainNavScreen,
    onChat: () -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
) {
    if (screen == MainNavScreen.News) {
        TopAppBar(
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(56.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        IconButton(onClick = onChat) {
                            IconChatBot()
                        }
                    }

                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        BrandMastHead()
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(56.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        IconButton(onClick = onSearch) {
                            IconSearch()
                        }
                    }
                }
            },
            elevation = Dimens.zero,
        )
        return
    }

    TopAppBar(
        title = {
            Text(text = stringResource(id = screen.titleId))
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
        onChat = { /*TODO*/ },
        onSearch = { /*TODO*/ }
    ) {

    }
}
