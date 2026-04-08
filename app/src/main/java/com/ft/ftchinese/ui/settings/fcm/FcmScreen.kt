package com.ft.ftchinese.ui.settings.fcm

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.repository.NotificationSettingStatus
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.ListItemIconText
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.IconRightArrow
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun FcmScreen(
    loading: Boolean,
    notificationStatus: NotificationSettingStatus,
    hasPromptedOnce: Boolean,
    messageRows: List<IconTextRow>,
    onToggleNotification: (Boolean) -> Unit,
    onSetting: () -> Unit,
    onCheck: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.fcm_check_guide),
                modifier = Modifier.padding(Dimens.dp16),
                style = MaterialTheme.typography.body2,
                color = OColor.black60,
            )

            ClickableRow(
                endIcon = {
                    Switch(
                        checked = notificationStatus.enabled,
                        onCheckedChange = onToggleNotification,
                    )
                },
                onClick = {
                    onToggleNotification(!notificationStatus.enabled)
                },
                contentPadding = PaddingValues(Dimens.dp16),
                background = OColor.black5
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.notification_system_title),
                        style = MaterialTheme.typography.h6,
                    )
                    Spacer(modifier = Modifier.height(Dimens.dp8))
                    Text(
                        text = stringResource(
                            id = notificationSummaryText(
                                notificationStatus = notificationStatus,
                                hasPromptedOnce = hasPromptedOnce,
                            )
                        ),
                        style = MaterialTheme.typography.body2,
                        color = OColor.black60,
                    )
                }
            }

            ClickableRow(
                endIcon = { IconRightArrow() },
                onClick = onSetting,
                contentPadding = PaddingValues(Dimens.dp16),
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.channel_setting_news),
                        style = MaterialTheme.typography.h6,
                    )
                    Spacer(modifier = Modifier.height(Dimens.dp8))
                    Text(
                        text = stringResource(id = R.string.notification_system_setting_hint),
                        style = MaterialTheme.typography.body2,
                        color = OColor.black60,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(Dimens.dp16)
            ) {

                messageRows.forEach { row ->
                    ListItemIconText(
                        icon = painterResource(id = row.icon),
                        text = stringResource(id = row.text),
                        iconTint = OColor.claret
                    )

                    Spacer(modifier = Modifier.height(Dimens.dp16))
                }
            }
        }

        PrimaryButton(
            onClick = onCheck,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.dp16),
            enabled = !loading,
            text = stringResource(id = R.string.fcm_start_checking)
        )
    }
}

data class IconTextRow(
    @DrawableRes val icon: Int,
    @StringRes val text: Int,
)

class FcmMessageBuilder {
    private var rows: List<IconTextRow> = listOf()

    fun addSystemNotification(status: NotificationSettingStatus): FcmMessageBuilder {
        val item = if (status.enabled) {
            IconTextRow(
                icon = R.drawable.ic_baseline_done_24,
                text = R.string.notification_status_enabled
            )
        } else {
            IconTextRow(
                icon = R.drawable.ic_error_outline_claret_24dp,
                text = R.string.notification_status_disabled
            )
        }
        rows = rows + listOf(item)

        return this
    }

    fun addPlayService(available: Boolean): FcmMessageBuilder {
        val item = if (available) {
            IconTextRow(
                icon = R.drawable.ic_baseline_done_24,
                text = R.string.play_service_available,
            )
        } else {
            IconTextRow(
                icon = R.drawable.ic_error_outline_claret_24dp,
                text = R.string.play_service_not_available
            )
        }
        rows = rows + listOf(item)

        return this
    }

    fun addTokenRetrievable(available: Boolean): FcmMessageBuilder {
        val item = if (available) {
            IconTextRow(
                icon = R.drawable.ic_baseline_done_24,
                text = R.string.fcm_accessible
            )
        } else {
            IconTextRow(
                icon = R.drawable.ic_error_outline_claret_24dp,
                text = R.string.fcm_inaccessible
            )
        }
        rows = rows + listOf(item)

        return this
    }

    fun build(): List<IconTextRow> {
        return rows
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFcmBody() {
    FcmScreen(
        loading = true,
        notificationStatus = NotificationSettingStatus(
            enabled = false,
            permissionGranted = false,
            appNotificationsEnabled = false,
            channelEnabled = true,
        ),
        hasPromptedOnce = false,
        messageRows = listOf(),
        onToggleNotification = {},
        onSetting = {},
        onCheck = {},
    )
}

@StringRes
private fun notificationSummaryText(
    notificationStatus: NotificationSettingStatus,
    hasPromptedOnce: Boolean,
): Int {
    return when {
        notificationStatus.enabled -> R.string.notification_status_enabled
        !notificationStatus.permissionGranted && !hasPromptedOnce -> R.string.notification_summary_first_request
        !notificationStatus.permissionGranted -> R.string.notification_summary_open_settings
        !notificationStatus.appNotificationsEnabled -> R.string.notification_summary_open_settings
        !notificationStatus.channelEnabled -> R.string.notification_summary_channel_disabled
        else -> R.string.notification_status_disabled
    }
}
