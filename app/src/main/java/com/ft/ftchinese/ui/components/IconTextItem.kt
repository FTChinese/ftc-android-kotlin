package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun IconTextItem(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
    iconTint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    textStyle: TextStyle = LocalTextStyle.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(painter = icon, contentDescription = "", tint = iconTint)
        Text(
            text = text,
            modifier = Modifier.padding(start = Dimens.dp16),
            style = textStyle,
        )
    }
}
