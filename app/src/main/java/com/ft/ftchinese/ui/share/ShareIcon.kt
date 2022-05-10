package com.ft.ftchinese.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
        modifier = Modifier.clickable(
            onClick = onClick
        )
    ) {
        Image(
            painter = image,
            contentDescription = text,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(64.dp)
                .width(64.dp)
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        Text(
            text = text,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
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
