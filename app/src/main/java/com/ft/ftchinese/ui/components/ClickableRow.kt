package com.ft.ftchinese.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ClickableRow(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailIcon: Painter? = painterResource(
        id = R.drawable.ic_keyboard_arrow_right_gray_24dp
    ),
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .fillMaxWidth()
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()

        trailIcon?.let {
            Icon(painter = it, contentDescription = "")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewClickableRow() {
    ClickableRow(
        onClick = {  },
        modifier = Modifier
            .padding(
                horizontal = Dimens.dp16,
                vertical = Dimens.dp8
            )
    ) {
        Text(text = "Item 1")
    }
}
