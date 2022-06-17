package com.ft.ftchinese.ui.main.home

import androidx.compose.foundation.Image
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.IconSearch
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun MainToolBar(
    screen: MainNavScreen,
    onSearch: () -> Unit
) {
    TopAppBar(
        title = {
            when (screen) {
                MainNavScreen.News -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_menu_masthead),
                        contentDescription = "",
                        contentScale = ContentScale.Fit
                    )
                }
                else -> {
                    Text(text = stringResource(id = screen.titleId))
                }
            }
        },
        elevation = Dimens.dp4,
        backgroundColor = OColor.wheat,
        actions = {
            IconButton(
                onClick = onSearch
            ) {
                IconSearch()
            }
        }
    )
}
