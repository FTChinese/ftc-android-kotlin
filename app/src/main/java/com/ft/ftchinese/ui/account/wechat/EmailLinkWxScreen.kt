package com.ft.ftchinese.ui.account.wechat

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.theme.Dimens

/**
 * Used when email is not linked to wechat.
 */
@Composable
fun EmailLinkWxScreen(
    onLinkWx: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.dp16)
    ) {
        Text(
            text = "尚未关联微信。绑定微信账号后，可以使用微信账号账号快速登录"
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))
        PrimaryButton(
            onClick = onLinkWx,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "微信授权")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAlertEmailLinkWx() {
    EmailLinkWxScreen {

    }
}
