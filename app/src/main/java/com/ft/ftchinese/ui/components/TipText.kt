package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun TipText(
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2,
        color = OColor.black60,
        modifier = Modifier.fillMaxWidth().then(modifier),
        textAlign = textAlign,
    )
}
