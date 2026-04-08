package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun Toolbar(
    heading: String,
    onBack: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    TopAppBar(
        title = {
            Text(
                text = heading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                IconArrowBackIOS()
            }
        },
        actions = actions,
        elevation = Dimens.dp4,
    )
}

@Preview
@Composable
fun PreviewToolbar() {
    Toolbar(
        heading = "Title",
        onBack = {},
    )
}

@Composable
fun CloseBar(
    title: String = "",
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onClose
            ) {
                IconClose()
            }
        },
        actions = actions,
        elevation = 0.dp
    )
}
