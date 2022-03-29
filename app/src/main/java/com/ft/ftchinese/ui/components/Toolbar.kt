package com.ft.ftchinese.ui.components

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun Toolbar(
    onBack: () -> Unit,
    currentScreen: SubsScreen,
    isMenu: Boolean = false,
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
                    if (isMenu) Icons.Filled.ArrowBack else Icons.Filled.ArrowBack,
                    ""
                )
            }
        },
        elevation = Dimens.dp4,
        backgroundColor = OColor.wheat
    )
}

@Preview
@Composable
fun PreviewToolbar() {
    Toolbar(
        currentScreen = SubsScreen.FtcPay,
        onBack = {},
    )
}

