package com.ft.ftchinese.ui.release

import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OFont

@Composable
fun ReleaseScreen(
    loading: Boolean,
    downloadStatus: DownloadStatus,
    newRelease: AppRelease?,
    currentVersion: Int,
    modifier: Modifier = Modifier,
    onClick: (DownloadStatus, AppRelease) -> Unit,
) {

    ProgressLayout(
        loading = loading,
        modifier = modifier,
    ) {
        newRelease?.let {
            val isNew = newRelease.versionCode > currentVersion

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {

                Spacer(modifier = Modifier.height(Dimens.dp16))

                Text(
                    text = if (isNew) {
                        stringResource(
                            R.string.found_new_release,
                            newRelease.versionName
                        )
                    } else {
                        stringResource(
                            R.string.already_latest_release
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.h6
                )

                if (isNew) {
                    PrimaryButton(
                        onClick = {
                            onClick(downloadStatus, newRelease)
                        },
                        modifier = Modifier
                            .padding(Dimens.dp16)
                            .fillMaxWidth(),
                        enabled = downloadStatus != DownloadStatus.Progress,
                    ) {
                        Text(
                            text = stringResource(id = downloadStatus.id),
                            fontSize = OFont.blockButton,
                        )
                    }
                }
            }

        }
    }
}

enum class DownloadStatus(val id: Int) {
    NotStarted(R.string.btn_download_now),
    Progress(R.string.btn_downloading),
    Completed(R.string.btn_download_complete)
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
