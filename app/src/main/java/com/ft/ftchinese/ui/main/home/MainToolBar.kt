package com.ft.ftchinese.ui.main.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.IconArrowBackIOS
import com.ft.ftchinese.ui.components.IconSearch
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

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
                            .width(104.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        ChatFTCButton(onClick = onChat)
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
private fun ChatFTCButton(
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(3.dp)
    Row(
        modifier = Modifier
            .width(92.dp)
            .height(30.dp)
            .clip(shape)
            .background(OColor.wheat)
            .border(width = 1.dp, color = OColor.wheat, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_chatftc_sparkles_24),
            contentDescription = null,
            tint = OColor.black80,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "ChatFTC",
            color = OColor.black80,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
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
