package com.ft.ftchinese.ui.main.myft

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    MyFtRow.Paywall,
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
    onClick: (MyFtRow) -> Unit,
    header: @Composable ColumnScope.() -> Unit,
) {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {

        header()

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

@Composable
fun Avatar(
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    displayName: String?,
    imageUrl: String?,
    placeholder: Painter = painterResource(id = R.drawable.ic_account_circle_black_24dp),
    imageSize: Dp = 128.dp,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(
                painter = placeholder,
                contentDescription = "avatar",
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(10.dp)),
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = "",
                placeholder = painterResource(id = R.drawable.ic_account_circle_black_24dp),
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit,
            )
        }

        if (loggedIn) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        onClick = onLogout
                    )
            ) {
                SubHeading2(text = displayName ?: "")
                Spacer(modifier = Modifier.width(Dimens.dp4))
                Icon(painter = painterResource(id = R.drawable.ic_exit_to_app_black_24dp), contentDescription = "Logout")
            }
        } else {
            PrimaryButton(
                onClick = onLogin,
                text = stringResource(id = R.string.title_login_signup),
            )
        }
    }
}

@Composable
fun AlertLogout(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PrimaryButton(
                onClick = onConfirm
            ) {
                Text(text = stringResource(id = R.string.btn_ok))
            }
        },
        dismissButton = {
            PlainTextButton(
                onClick = onDismiss,
                text = stringResource(id = R.string.btn_cancel)
            )
        },
        text = {
            Text(text = stringResource(id = R.string.action_logout))
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAvatar() {
    Avatar(
        loggedIn = true,
        onLogin = { },
        onLogout = { },
        displayName = "Hello World",
        imageUrl = null
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMyFtScreen() {
    MyFtScreen(
        loggedIn = true,
        onClick = {}
    ) {

    }
}
