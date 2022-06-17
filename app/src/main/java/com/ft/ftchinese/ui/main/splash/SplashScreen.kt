package com.ft.ftchinese.ui.main.splash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ft.ftchinese.ui.components.OButton
import com.ft.ftchinese.ui.components.OButtonDefaults
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun SplashScreen(
    splash: SplashShown?,
    counter: Int,
    onClickCounter: () -> Unit,
    onClickImage: (url: String) -> Unit,
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        splash?.let {
            AsyncImage(
                model = ImageRequest
                    .Builder(LocalContext.current)
                    .data(splash.image)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClick = {
                            onClickImage(splash.ad.linkUrl)
                        },
                    ),
                contentScale = ContentScale.Fit,
            )
        }

        if (counter > 0) {
            OButton(
                onClick = onClickCounter,
                modifier = Modifier
                    .align(Alignment.TopEnd),
                border = BorderStroke(1.dp, OColor.black60),
                colors = OButtonDefaults.outlineButtonColors(
                    contentColor = OColor.black60
                ),
                contentPadding = PaddingValues(
                    horizontal = Dimens.dp16,
                    vertical = Dimens.dp4
                ),
                margin = PaddingValues(Dimens.dp24),
            ) {
                Text(
                    text = "$counter",
                    modifier = Modifier.align(Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSplashScreen() {
    SplashScreen(
        splash = null,
        counter = 5,
        onClickCounter = {},
        onClickImage = {}
    )
}
