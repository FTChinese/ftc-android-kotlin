package com.ft.ftchinese.ui.account.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.ui.account.AccountRow
import com.ft.ftchinese.ui.account.AccountRowId
import com.ft.ftchinese.ui.account.buildAccountRows
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun FtcAccountScreen(
    rows: List<AccountRow>,
    onClickRow: (AccountRowId) -> Unit
) {

    Column(
        modifier = Modifier.verticalScroll(
            rememberScrollState()
        )
    ) {
        rows.forEach { row ->
            AccountRow(
                primary = row.primary,
                secondary = row.secondary
            ) {
                onClickRow(row.id)
            }
        }

        SecondaryButton(
            onClick = {
                onClickRow(AccountRowId.DELETE)
            },
            modifier = Modifier
                .padding(Dimens.dp16)
                .fillMaxWidth(),
            colors = OButtonDefaults.outlineButtonDanger(),
        ) {
            Text(
                text = stringResource(id = R.string.title_delete_account)
            )
        }
    }
}

@Composable
private fun AccountRow(
    primary: String,
    secondary: String,
    onClick: () -> Unit
) {
    ClickableRow(
        onClick = onClick,
        endIcon = {
            IconRightArrow()
        },
    ) {
        ListItemTwoLine(
            primary = primary,
            secondary = secondary
        )
    }
}

@Composable
fun MobileOnlyNotUpdatable(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PrimaryButton(
                onClick = onDismiss,
                text = stringResource(id = R.string.btn_ok),
            )
        },
        text = {
            Text(text = "手机号创建的账号不允许更改")
        }
    )
}

@Composable
fun AlertDeleteAccount(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    SimpleDialog(
        title = stringResource(id = R.string.title_confirm_delete_account),
        body = stringResource(id = R.string.message_warn_delete_account),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = stringResource(id = R.string.button_delete_account),
        dismissText = stringResource(id = R.string.button_think_twice)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewFtcAccountScreen() {
    val context = LocalContext.current

    FtcAccountScreen(
        rows = buildAccountRows(
            context,
            Account(
                id = "",
                email = "example@example.org",
                isVerified = false,
                wechat = Wechat(),
                membership = Membership(),
            )
        ),
        onClickRow = {}
    )
}
