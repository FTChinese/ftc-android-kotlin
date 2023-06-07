package com.ft.ftchinese.ui.settings.release

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.IconDelete
import com.ft.ftchinese.ui.components.IconFolderOpen
import com.ft.ftchinese.ui.components.IconRefresh
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun ReleaseMenu(
    checkOnLaunch: Boolean,
    onToggleCheck: (Boolean) -> Unit,
    onDeleteDownload: () -> Unit,
    onOpenDownloadsFolder: () -> Unit,
    onRefreshRelease: () -> Unit,
    progress: Boolean
) {
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        ClickableRow(
            onClick = {  },
            contentPadding = PaddingValues(Dimens.dp16),
            endIcon = {
                Switch(
                    checked = checkOnLaunch,
                    onCheckedChange = onToggleCheck,
                )
            }
        ) {
            Text(
                text = "App启动时检查新版本",
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        ClickableRow(
            onClick = onOpenDownloadsFolder,
            contentPadding = PaddingValues(Dimens.dp16),
            endIcon = {
                IconFolderOpen()
            }
        ) {
            Text(
                text = "打开下载文件夹",
            )
        }

        ClickableRow(
            onClick = onDeleteDownload,
            endIcon = {
                IconDelete()
            },
            contentPadding = PaddingValues(Dimens.dp16)
        ) {
            Text(
                text = "删除下载的文件",
            )
        }

        ClickableRow(
            onClick = onRefreshRelease,
            endIcon = {
                IconRefresh()
            },
            contentPadding = PaddingValues(Dimens.dp16),
            enabled = !progress
        ) {
            Text(
                text = "重新检查",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReleaseMenu() {
    ReleaseMenu(
        onDeleteDownload = {},
        onOpenDownloadsFolder = {},
        onToggleCheck = {},
        onRefreshRelease = {},
        checkOnLaunch = true,
        progress = false,
    )
}
