package com.ft.ftchinese.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.RightArrow
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor


@Composable
fun PreferenceScreen(
    cacheSize: String?,
    readCount: Int?,
    onClickRow: (SettingScreen) -> Unit
) {
    Column {
        PreferenceItem(
            title = stringResource(id = R.string.pref_clear_cache),
            summary = cacheSize ?: "0 KiB",
            leadIcon = painterResource(id = R.drawable.ic_clear_24dp),
            onClick = {
                onClickRow(SettingScreen.ClearCache)
            }
        )

        PreferenceItem(
            title = stringResource(id = R.string.pref_clear_history),
            summary = readCount?.let {
                stringResource(R.string.summary_articles_read, it)
            } ?: "",
            leadIcon = painterResource(id = R.drawable.ic_delete_forever_black_24dp),
            onClick = {
                onClickRow(SettingScreen.ClearHistory)
            }
        )

        PreferenceItem(
            title = stringResource(id = R.string.fcm_pref),
            summary = stringResource(id = R.string.fcm_summary),
            leadIcon = painterResource(id = R.drawable.ic_notifications_black_24dp),
            trailIcon = true,
            onClick = {
                onClickRow(SettingScreen.Notification)
            }
        )

        PreferenceItem(
            title = stringResource(id = R.string.pref_check_new_version),
            summary = stringResource(R.string.current_version, BuildConfig.VERSION_NAME),
            leadIcon = painterResource(id = R.drawable.ic_update_black_24dp),
            trailIcon = true,
            onClick = {
                onClickRow(SettingScreen.CheckVersion)
            },
        )
    }
}

@Composable
fun PreferenceItem(
    title: String,
    summary: String?,
    leadIcon: Painter,
    trailIcon: Boolean = false,
    onClick: () -> Unit,
) {
    ClickableRow(
        modifier = Modifier.padding(Dimens.dp16),
        startIcon = {
            Icon(painter = leadIcon, contentDescription = title)
        },
        endIcon = { if (trailIcon) { RightArrow() } },
        onClick = onClick
    ) {

        Column(
            modifier = Modifier
                .padding(
                    start = Dimens.dp8,
                    end = Dimens.dp8
                )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
            )
            summary?.let {
                Text(
                    text = it,
                    color = OColor.black60,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = Dimens.dp8)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPreferenceBody() {
    PreferenceScreen(
        cacheSize = "11.2kb",
        readCount = 5,
        onClickRow = {}
    )
}
