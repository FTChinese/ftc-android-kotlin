package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun Heading1(
    text: String,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = OColor.black60
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = textAlign,
        style = MaterialTheme.typography.h4,
        color = color,
    )
}

@Composable
fun Heading2(
    text: String,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = textAlign,
        style = MaterialTheme.typography.h5,
        color = color,
    )
}

@Composable
fun Heading3(
    text: String,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = textAlign,
        style = MaterialTheme.typography.h6,
        color = color,
    )
}

@Composable
fun SubHeading1(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        textAlign = textAlign,
        style = MaterialTheme.typography.subtitle1,
        color = color,
    )
}

@Composable
fun SubHeading2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = MaterialTheme.typography.subtitle2,
        color = color,
    )
}
