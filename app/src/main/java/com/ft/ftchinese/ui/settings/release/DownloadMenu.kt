package com.ft.ftchinese.ui.settings.release

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
fun DownloadMenu(
    loading: Boolean,
    downloadStage: DownloadStage,
    release: AppRelease,
    onClick: (AppRelease) -> Unit, // The click action wil pass the variable newRelease back to parent.
) {
    Column(
        modifier = Modifier.padding(Dimens.dp16)
    ) {

        if (release.isNew) {
            Heading3(
                text = stringResource(
                    R.string.found_new_release,
                    release.versionName
                ),
                color = OColor.black
            )

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
        } else {
            Heading3(
                text = stringResource(
                    R.string.already_latest_release
                ),
                color = OColor.black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDownloadNotStarted() {
    DownloadMenu(
        loading = false,
        downloadStage = DownloadStage.NotStarted,
        release = AppRelease(
            versionName = "v6.8.3",
            versionCode = 113,
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDownloadProgress() {
    DownloadMenu(
        loading = false,
        downloadStage = DownloadStage.Progress,
        release = AppRelease(
            versionName = "v6.8.3",
            versionCode = 113,
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDownloadMenuCompleted() {
    DownloadMenu(
        loading = false,
        downloadStage = DownloadStage.Completed(1),
        release = AppRelease(
            versionName = "v6.8.3",
            versionCode = 113,
        ),
        onClick = {}
    )
}
