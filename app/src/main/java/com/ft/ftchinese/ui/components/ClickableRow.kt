package com.ft.ftchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.ui.theme.Dimens

data class TableGroup<T>(
    val header: String,
    val rows: List<T>
)

@Composable
fun ClickableRow(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    margin: PaddingValues = PaddingValues(0.dp),
    contentPadding: PaddingValues = PaddingValues(Dimens.dp16),
    background: Color = Color.Transparent,
    startIcon: @Composable RowScope.() -> Unit = {},
    endIcon: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(margin)
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .background(background)
            .padding(contentPadding)
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
            IconRightArrow()
        },
        onClick = {  }
    ) {
        Text(text = "Item 1")
    }
}
