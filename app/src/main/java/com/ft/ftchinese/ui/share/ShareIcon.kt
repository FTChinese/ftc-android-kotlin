package com.ft.ftchinese.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ShareIcon(
    image: Painter,
    text: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(Dimens.dp16)
    ) {
        Image(
            painter = image,
            contentDescription = text,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(32.dp)
                .width(32.dp)
                .clickable(
                    onClick = onClick
                )
        )

        Spacer(modifier = Modifier.height(Dimens.dp8))

        Text(
            text = text,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun SocialShareList(
    apps: List<SocialApp>,
    onShareTo: (SocialApp) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        apps.forEach { app ->
            ShareIcon(
                image = painterResource(id = app.icon),
                text = "${app.name}"
            ) {
                onShareTo(app)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewShareIcon() {
    ShareIcon(
        image = painterResource(id = R.drawable.wechat),
        text = "微信"
    ) {
        
    }
}
