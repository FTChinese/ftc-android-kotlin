package com.ft.ftchinese.ui.settings.release

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ReleaseScreen(
    menu: @Composable () -> Unit,
    downloadButton: @Composable () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {

        menu()

        Divider(startIndent = Dimens.dp16)

        downloadButton()
    }
}

@Composable
fun AlertDownloadStart(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(id = R.string.btn_ok)
                )
            }
        },
        text = {
            Text(text = stringResource(id = R.string.wait_download_finish))
        }
    )
}


@Preview(showBackground = true)
@Composable
fun PreviewReleaseScreen() {
    ReleaseScreen(
        menu = {},
        downloadButton = {}
    )
}
