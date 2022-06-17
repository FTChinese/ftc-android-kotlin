package com.ft.ftchinese.ui.settings.release

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.IconDelete
import com.ft.ftchinese.ui.components.IconFolderOpen
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ReleaseMenu(
    onClickDelete: () -> Unit,
    onViewDownloads: () -> Unit
) {
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        ClickableRow(
            onClick = onClickDelete,
            startIcon = {
                IconDelete()
            },
            contentPadding = PaddingValues(Dimens.dp16)
        ) {
            Text(
                text = "删除下载的文件",

            )
        }

        ClickableRow(
            onClick = onViewDownloads,
            startIcon = {
                IconFolderOpen()
            },
            contentPadding = PaddingValues(Dimens.dp16)
        ) {
            Text(
                text = "打开下载文件夹",

            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReleaseMenu() {
    ReleaseMenu(
        onClickDelete = {}
    ) {

    }
}
