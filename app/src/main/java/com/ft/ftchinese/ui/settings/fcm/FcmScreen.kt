package com.ft.ftchinese.ui.settings.fcm

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.ListItemIconText
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.IconRightArrow
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun FcmScreen(
    loading: Boolean,
    messageRows: List<IconTextRow>,
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
                endIcon = { IconRightArrow() },
                onClick = onSetting,
                contentPadding = PaddingValues(Dimens.dp16),
                background = OColor.black5
            ) {
                Text(
                    text = stringResource(id = R.string.channel_setting_news),
                    style = MaterialTheme.typography.h6,
                )
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

    fun addPlayService(available: Boolean): FcmMessageBuilder {
        val item = if (available) {
            IconTextRow(
                icon = R.drawable.ic_done_claret_24dp,
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
                icon = R.drawable.ic_done_claret_24dp,
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
        onSetting = {},
        messageRows = listOf()
    ) {}
}
