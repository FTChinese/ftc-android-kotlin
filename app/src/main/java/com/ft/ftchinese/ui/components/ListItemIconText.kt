package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ListItemIconText(
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
        Icon(
            painter = icon,
            contentDescription = "",
            tint = iconTint
        )

        Spacer(modifier = Modifier.width(Dimens.dp4))

        Text(
            text = text,
            modifier = Modifier.padding(start = Dimens.dp16),
            style = textStyle,
        )
    }
}
