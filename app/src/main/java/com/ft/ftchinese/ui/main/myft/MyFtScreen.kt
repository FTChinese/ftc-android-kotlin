package com.ft.ftchinese.ui.main.myft

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.test.TestActivity
import com.ft.ftchinese.ui.theme.Dimens

enum class MyFtRow(
    @StringRes val textId: Int,
) {
    Account(R.string.title_account),
    Paywall(R.string.title_subscription),
    MySubs(R.string.title_my_subs),
    Settings(R.string.title_settings),
    ReadHistory(R.string.myft_reading_history),
    Bookmarked(R.string.myft_starred_articles),
    Topics(R.string.myft_following);
}

val loggedInRows: List<MyFtRow> = listOf(
    MyFtRow.Account,
    MyFtRow.MySubs,
)

val toolsRows: List<MyFtRow> = listOf(
    MyFtRow.MySubs,
    MyFtRow.Settings,
)

val articleRows: List<MyFtRow> = listOf(
    MyFtRow.ReadHistory,
    MyFtRow.Bookmarked,
    MyFtRow.Topics
)



fun buildMyFtRows(loggedIn: Boolean): List<TableGroup<MyFtRow>> {
    return if (loggedIn) {
        listOf(
            TableGroup(
                header = "",
                rows = loggedInRows
            )
        )
    } else {
        listOf()
    } + listOf(
        TableGroup(
            header = "",
            rows = toolsRows
        ),
        TableGroup(
            header = "",
            rows = articleRows
        )
    )
}

@Composable
fun MyFtScreen(
    loggedIn: Boolean,
    avatarUrl: String?,
    displayName: String?,
    onLogin: () -> Unit,
    onClick: (MyFtRow) -> Unit
) {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            if (avatarUrl.isNullOrBlank()) {
                Figure(
                    caption = displayName ?: ""
                )
            } else {
                Figure(
                    imageUrl = avatarUrl,
                    caption = displayName ?: ""
                )
            }
        }

        if (!loggedIn) {
            PrimaryButton(
                onClick = onLogin,
                text = stringResource(id = R.string.title_login_signup),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.dp8))
        Divider()

        buildMyFtRows(loggedIn).forEachIndexed { index, tableGroup ->

            if (index != 0) {
                Divider(startIndent = Dimens.dp8)
            }

            tableGroup.rows.forEach { row ->
                ClickableRow(
                    onClick = { onClick(row) },
                    endIcon = {
                        IconRightArrow()
                    },
                ) {
                    SubHeading2(text = stringResource(id = row.textId))
                }
            }
        }

        if (BuildConfig.DEBUG) {
            Divider()
            
            ClickableRow(
                onClick = { TestActivity.start(context) },
                endIcon = {
                    IconRightArrow()
                }
            ) {
                SubHeading2(text = "Test")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyFtScreen() {
    MyFtScreen(
        loggedIn = true,
        avatarUrl = null,
        displayName = "Hello",
        onLogin = {},
        onClick = {}
    )
}
