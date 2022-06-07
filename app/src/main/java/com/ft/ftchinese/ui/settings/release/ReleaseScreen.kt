package com.ft.ftchinese.ui.settings.release

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.ui.components.BlockButton
import com.ft.ftchinese.ui.components.BodyText2
import com.ft.ftchinese.ui.components.Heading3
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OButton
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ReleaseScreen(
    loading: Boolean,
    downloadStage: DownloadStage,
    newRelease: AppRelease?,
    onClick: (AppRelease) -> Unit,
    onDelete: (Long) -> Unit,
) {

    newRelease?.let { release ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.dp16),
        ) {

            Spacer(modifier = Modifier.height(Dimens.dp16))

            Heading3(
                text = if (release.isNew) {
                    stringResource(
                        R.string.found_new_release,
                        release.versionName
                    )
                } else {
                    stringResource(
                        R.string.already_latest_release
                    )
                },
                color = OColor.black
            )

            if (release.isNew) {
                Spacer(modifier = Modifier.height(Dimens.dp16))

                BlockButton(
                    onClick = {
                        onClick(newRelease)
                    },

                    enabled = !loading && (downloadStage !is DownloadStage.Progress),
                    text = when (downloadStage) {
                        is DownloadStage.NotStarted -> {
                            stringResource(id = R.string.btn_download_now)
                        }
                        is DownloadStage.Progress -> {
                            stringResource(id = R.string.btn_downloading)
                        }
                        is DownloadStage.Completed -> {
                            stringResource(id = R.string.btn_download_complete)
                        }
                    }
                )
            }

            if (downloadStage is DownloadStage.Completed) {
                Spacer(modifier = Modifier.height(Dimens.dp4))

                DeleteApk {
                    onDelete(downloadStage.downloadId)
                }
            }
        }
    }
}

@Composable
private fun DeleteApk(
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.End),
            colors = OButton.textColors(
                contentColor = OColor.claret
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete_forever_black_24dp),
                contentDescription = "Delete",
            )
            Text(text = "删除下载的文件")
        }

        BodyText2(
            text = "下载文件位于手机存储/Download文件夹下",
            textAlign = TextAlign.End
        )
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
        loading = false,
        downloadStage = DownloadStage.Completed(1),
        newRelease = AppRelease(
            versionName = "v6.9.0",
            versionCode = 100,
            apkUrl = "https://www.ftchinese.com"
        ),
        onClick = {},
        onDelete = {}
    )
}
