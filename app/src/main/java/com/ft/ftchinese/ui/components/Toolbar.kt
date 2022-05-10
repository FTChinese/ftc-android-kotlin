package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun Toolbar(
    currentScreen: SubsScreen,
    isMenu: Boolean = false,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = currentScreen.titleId)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = if (isMenu) Icons.Filled.Menu else Icons.Filled.ArrowBack,
                    "",
                    tint = OColor.black90
                )
            }
        },
        elevation = Dimens.dp4,
        backgroundColor = OColor.wheat
    )
}

@Composable
fun Toolbar(
    heading: String,
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
                    imageVector = Icons.Filled.ArrowBack,
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

@Composable
fun CloseBar(
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                "",
                tint = OColor.black90
            )
        }
    }
}

@Preview
@Composable
fun PreviewToolbar() {
    Toolbar(
        currentScreen = SubsScreen.FtcPay,
        onBack = {},
    )
}


