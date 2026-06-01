package com.ft.ftchinese.ui.main.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.IconArrowBackIOS
import com.ft.ftchinese.ui.components.IconChatBot
import com.ft.ftchinese.ui.components.IconSearch
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import kotlinx.coroutines.delay

private const val CHAT_FTC_HINT_PREFS = "chat_ftc_hint"
private const val CHAT_FTC_HINT_SHOWN = "chat_ftc_hint_shown_v2"
private const val CHAT_FTC_HINT_DURATION_MS = 3200L

@Composable
fun MainToolBar(
    screen: MainNavScreen,
    onChat: () -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
) {
    if (screen == MainNavScreen.News) {
        val context = LocalContext.current
        var showChatHint by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val prefs = context.applicationContext.getSharedPreferences(
                CHAT_FTC_HINT_PREFS,
                Context.MODE_PRIVATE
            )
            if (!prefs.getBoolean(CHAT_FTC_HINT_SHOWN, false)) {
                showChatHint = true
                prefs.edit().putBoolean(CHAT_FTC_HINT_SHOWN, true).apply()
                delay(CHAT_FTC_HINT_DURATION_MS)
                showChatHint = false
            }
        }

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

                    ChatFTCHintBubble(
                        visible = showChatHint,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = 40.dp)
                            .zIndex(1f)
                    )
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
    IconButton(onClick = onClick) {
        IconChatBot(tint = OColor.black80)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ChatFTCHintBubble(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.96f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .width(6.dp)
                    .height(10.dp)
            ) {
                val path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(0f, size.height / 2f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(path, OColor.paper)
            }
            Row(
                modifier = Modifier
                    .shadow(2.dp, shape)
                    .background(OColor.paper, shape)
                    .border(width = 1.dp, color = OColor.black20, shape = shape)
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chatftc_sparkles_24),
                    contentDescription = null,
                    tint = OColor.black80,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "ChatFTC",
                    color = OColor.black80,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
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
