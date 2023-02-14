package com.ft.ftchinese.ui.settings.release

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.ui.components.Heading3
import com.ft.ftchinese.ui.components.PrimaryBlockButton
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ReleaseScreen(
    loading: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    downloadStage: DownloadStage,
    newRelease: AppRelease?,
    onClick: (AppRelease) -> Unit, // The click action wil pass the variable newRelease back to parent.
    onDelete: (Long) -> Unit,
    onViewDownloads: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "App启动时检查新版本",
                modifier = Modifier
                    .weight(1f)
                    .padding(Dimens.dp16)
            )

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }

        Divider(startIndent = Dimens.dp16)

        Column(
            modifier = Modifier.padding(Dimens.dp16)
        ) {
            newRelease?.let { release ->

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

                    PrimaryBlockButton(
                        onClick = {
                            onClick(release)
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
                        },
                    )
                }
            }
        }

        if (downloadStage is DownloadStage.Completed) {
            Divider(startIndent = Dimens.dp16)

            ReleaseMenu(
                onClickDelete = {
                    onDelete(downloadStage.downloadId)
                },
                onViewDownloads = onViewDownloads
            )
        }

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
        checked = true,
        onCheckedChange = {},
        downloadStage = DownloadStage.Completed(1),
        newRelease = AppRelease(
            versionName = "v6.9.0",
            versionCode = 100,
            apkUrl = "https://www.ftchinese.com"
        ),
        onClick = {},
        onDelete = {},
        onViewDownloads = {}
    )
}
