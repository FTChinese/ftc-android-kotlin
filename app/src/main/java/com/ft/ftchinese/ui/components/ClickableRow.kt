package com.ft.ftchinese.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun RightArrow() {
    Icon(
        painter = painterResource(id = R.drawable.ic_keyboard_arrow_right_gray_24dp),
        contentDescription = "Details"
    )
}

@Composable
fun ClickableRow(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    startIcon: @Composable () -> Unit = {},
    endIcon: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
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
        startIcon()

        Box(
            modifier = Modifier.weight(1f)
        ) {
            content()
        }

        endIcon()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewClickableRow() {
    ClickableRow(
        modifier = Modifier
            .padding(
                horizontal = Dimens.dp16,
                vertical = Dimens.dp8
            ),
        endIcon = {
            RightArrow()
        },
        onClick = {  }
    ) {
        Text(text = "Item 1")
    }
}
