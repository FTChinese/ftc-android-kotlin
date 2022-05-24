package com.ft.ftchinese.wxapi

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.ui.components.SecondaryButton
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun WxOAuthAScreen(
    status: AuthStatus,
    onFinish: () -> Unit,
    onLink: (Account) -> Unit,
    onRetry: () -> Unit,
) {
    when (status) {
        is AuthStatus.Loading -> {
            WxOAuthProgress(
                title = stringResource(id = R.string.progress_logging),
                subTitle = stringResource(id = R.string.wait_while_wx_login),
                buttonText = null,
                onClickButton = {}
            )
        }
        is AuthStatus.Failed -> {
            WxOAuthProgress(
                title = stringResource(id = R.string.prompt_login_failed),
                subTitle = status.message,
                onClickButton = onFinish
            )
        }
        is AuthStatus.NotConnected -> {
            WxOAuthProgress(
                title = "出错了",
                subTitle = stringResource(id = R.string.prompt_no_network),
                buttonText = "重试",
                onClickButton = onRetry
            )
        }
        is AuthStatus.LoginSuccess -> {
            WxOAuthProgress(
                title = stringResource(id = R.string.prompt_logged_in),
                subTitle = stringResource(
                    R.string.greeting_wx_login,
                    status.account.wechat.nickname ?: ""
                ),
                onClickButton = onFinish
            )
        }
        is AuthStatus.LinkLoaded -> {
            WxOAuthProgress(
                title = "授权成功！",
                subTitle = "关联微信${status.account.wechat.nickname}",
                buttonText = "关联账号",
                onClickButton = {
                    onLink(status.account)
                }
            )
        }
    }
}

@Composable
fun WxOAuthProgress(
    title: String,
    subTitle: String,
    buttonText: String? = stringResource(id = R.string.btn_done),
    onClickButton: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(Dimens.dp16)
            .fillMaxSize()
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))
        Text(
            text = subTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.body1
        )

        if (!buttonText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Dimens.dp16))
            SecondaryButton(
                onClick = onClickButton,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = buttonText)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWxOAuthScreenLoading() {
    WxOAuthAScreen(
        status = AuthStatus.Loading,
        onFinish = {},
        onLink = {}
    ) {

    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWxOAuthScreenFailed() {
    WxOAuthAScreen(
        status = AuthStatus.Failed("授权取消！"),
        onFinish = {},
        onLink = {}
    ) {

    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWxOAuthScreenSuccess() {
    WxOAuthAScreen(
        status = AuthStatus.LoginSuccess(
            Account(
                id = "",
                unionId = "wx-union-id",
                email = "",
                wechat = Wechat(
                    nickname = "Wechat user"
                ),
                membership = Membership()
            )
        ),
        onFinish = {},
        onLink = {}
    ) {

    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWxOAuthScreenLinkLoaded() {
    WxOAuthAScreen(
        status = AuthStatus.LinkLoaded(
            Account(
                id = "",
                unionId = "wx-union-id",
                email = "",
                wechat = Wechat(
                    nickname = "Wechat user"
                ),
                membership = Membership()
            )
        ),
        onFinish = {},
        onLink = {}
    ) {

    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWxOAuthScreenNotConnected() {
    WxOAuthAScreen(
        status = AuthStatus.NotConnected,
        onFinish = {},
        onLink = {}
    ) {

    }
}

