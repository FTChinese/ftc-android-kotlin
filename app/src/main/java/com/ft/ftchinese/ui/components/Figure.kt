package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ft.ftchinese.R

@Composable
fun Figure(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    imageSize: Dp = 128.dp,
    caption: String = "",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "",
            placeholder = painterResource(id = R.drawable.ic_account_circle_black_24dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(imageSize)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun Figure(
    painter: Painter = painterResource(id = R.drawable.ic_account_circle_black_24dp),
    modifier: Modifier = Modifier,
    imageSize: Dp = 128.dp,
    caption: String = "",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        Icon(
            painter = painter,
            contentDescription = "avatar",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(imageSize)
                .clip(RoundedCornerShape(10.dp)),
        )

        Text(
            text = caption,
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
