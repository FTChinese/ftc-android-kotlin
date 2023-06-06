package com.ft.ftchinese.ui.settings.overview

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.BodyText1
import com.ft.ftchinese.ui.components.BodyText2
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.IconRightArrow
import com.ft.ftchinese.ui.theme.Dimens

data class SettingRow(
    val id: SettingScreen,
    val summary: String?,
    @DrawableRes val iconId: Int,
    val disclosure: Boolean = false,
) {
    companion object {

        @JvmStatic
        fun build(
            resources: Resources,
            cacheSize: String,
            readCount: String
        ): List<SettingRow> {
            return listOf(
                SettingRow(
                    id = SettingScreen.FontSize,
                    summary = null,
                    iconId = R.drawable.baseline_format_size_black_24,
                    disclosure = true,
                ),
                SettingRow(
                    id = SettingScreen.ClearCache,
                    summary = cacheSize,
                    iconId = R.drawable.ic_baseline_clear_all_24,
                    disclosure = false,
                ),
                SettingRow(
                    id = SettingScreen.ClearHistory,
                    summary = readCount,
                    iconId = R.drawable.ic_delete_forever_black_24dp,
                    disclosure = false
                ),
                SettingRow(
                    id = SettingScreen.Notification,
                    summary = resources.getString(R.string.fcm_summary),
                    iconId = R.drawable.ic_notifications_black_24dp,
                    disclosure = true,
                ),
                SettingRow(
                    id = SettingScreen.CheckVersion,
                    summary = resources.getString(R.string.current_version, BuildConfig.VERSION_NAME),
                    iconId = R.drawable.ic_update_black_24dp,
                    disclosure = true
                ),
                SettingRow(
                    id = SettingScreen.AboutUs,
                    summary = null,
                    iconId = R.drawable.ic_info_black_24dp,
                    disclosure = true
                ),
                SettingRow(
                    id = SettingScreen.Feedback,
                    summary = null,
                    iconId = R.drawable.ic_feedback_black_24dp,
                    disclosure = false,
                ),
            )
        }
    }
}

@Composable
fun PreferenceRow(
    row: SettingRow,
    onClick: (SettingScreen) -> Unit,
) {
    ClickableRow(
        onClick = {
            onClick(row.id)
        },
        startIcon = {
            Icon(
                painter = painterResource(id = row.iconId),
                contentDescription = null
            )
        },
        endIcon = {
            if (row.disclosure) {
                IconRightArrow()
            }
        },
        contentPadding = PaddingValues(Dimens.dp16)
    ) {

        Column(
            modifier = Modifier
                .padding(
                    start = Dimens.dp8,
                    end = Dimens.dp8
                )
        ) {
            BodyText1(text = row.id.titleId.let { stringResource(id = it) })

            row.summary?.let {
                Spacer(modifier = Modifier.height(Dimens.dp8))
                BodyText2(text = it)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewPreferenceRow() {
    PreferenceRow(
        row = SettingRow(
            id = SettingScreen.Notification,
            summary = "接收或关闭通知推送",
            iconId = R.drawable.ic_notifications_black_24dp,
            disclosure = true,
        ),
        onClick = {}
    )
}
