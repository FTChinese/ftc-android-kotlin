package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// Size 16
@Composable
fun BodyText0(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    text: String,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        style = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            letterSpacing = 0.5.sp
        ),
        color = color,
        modifier = Modifier.fillMaxWidth().then(modifier),
        textAlign = textAlign,
    )
}

// Size 16
@Composable
fun BodyText1(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    text: String,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        style = MaterialTheme.typography.body1,
        color = color,
        modifier = Modifier.fillMaxWidth().then(modifier),
        textAlign = textAlign,
    )
}

// Size 14
@Composable
fun BodyText2(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    text: String,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2,
        color = color,
        modifier = Modifier.fillMaxWidth().then(modifier),
        textAlign = textAlign,
    )
}
