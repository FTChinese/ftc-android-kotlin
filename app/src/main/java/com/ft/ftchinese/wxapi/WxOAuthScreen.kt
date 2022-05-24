package com.ft.ftchinese.wxapi

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat

@Composable
fun WxOAuthAScreen(
    status: AuthStatus,
    onFinish: () -> Unit,
    onLink: (Account) -> Unit,
    onRetry: () -> Unit,
) {
    when (status) {
        is AuthStatus.Loading -> {
            WxRespProgress(
                title = stringResource(id = R.string.progress_logging),
                subTitle = stringResource(id = R.string.wait_while_wx_login),
                buttonText = null,
                onClickButton = {}
            )
        }
        is AuthStatus.Failed -> {
            WxRespProgress(
                title = stringResource(id = R.string.prompt_login_failed),
                subTitle = status.message,
                onClickButton = onFinish
            )
        }
        is AuthStatus.NotConnected -> {
            WxRespProgress(
                title = "出错了",
                subTitle = stringResource(id = R.string.prompt_no_network),
                buttonText = "重试",
                onClickButton = onRetry
            )
        }
        is AuthStatus.LoginSuccess -> {
            WxRespProgress(
                title = stringResource(id = R.string.prompt_logged_in),
                subTitle = stringResource(
                    R.string.greeting_wx_login,
                    status.account.wechat.nickname ?: ""
                ),
                onClickButton = onFinish
            )
        }
        is AuthStatus.LinkLoaded -> {
            WxRespProgress(
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

