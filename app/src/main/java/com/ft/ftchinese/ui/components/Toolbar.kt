package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun Toolbar(
    heading: String,
    icon: ImageVector = Icons.Filled.ArrowBack,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = heading
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = icon,
                    "",
                    tint = OColor.black90
                )
            }
        },
        actions = actions,
        elevation = Dimens.dp4,
        backgroundColor = OColor.wheat
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


