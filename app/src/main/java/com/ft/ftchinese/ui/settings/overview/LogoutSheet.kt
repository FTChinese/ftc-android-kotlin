package com.ft.ftchinese.ui.settings.overview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.IconExit
import com.ft.ftchinese.ui.components.SubHeading2
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun LogoutSheet(
    onClick: () -> Unit
) {
    ClickableRow(
        onClick = onClick,
        endIcon = {
            IconExit()
        },
        contentPadding = PaddingValues(Dimens.dp16)
    ) {
        SubHeading2(text = stringResource(id = R.string.action_logout))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLogoutSheet() {
    LogoutSheet {

    }
}
