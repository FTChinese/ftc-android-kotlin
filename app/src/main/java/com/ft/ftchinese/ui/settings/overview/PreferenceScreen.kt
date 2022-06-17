package com.ft.ftchinese.ui.settings.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.SecondaryButton

@Composable
fun PreferenceScreen(
    rows: List<SettingRow> = listOf(),
    onClickRow: (SettingScreen) -> Unit,
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        rows.forEach { row ->
            PreferenceRow(
                row = row,
                onClick = onClickRow
            )
        }

        if (isLoggedIn) {
            SecondaryButton(
                onClick = onLogout,
                text = stringResource(id = R.string.action_logout),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPreferenceBody() {
    val context = LocalContext.current
    PreferenceScreen(
        rows = SettingRow.build(
            context.resources,
            cacheSize = "11.2kb",
            readCount = context.getString(R.string.summary_articles_read, 10),
        ),
        onClickRow = {},
        isLoggedIn = true,
        onLogout = {}
    )
}
