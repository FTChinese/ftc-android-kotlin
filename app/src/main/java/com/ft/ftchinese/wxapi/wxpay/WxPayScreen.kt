package com.ft.ftchinese.wxapi.wxpay

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.wxapi.shared.WxRespProgress

@Composable
fun WxPayScreen(
    status: WxPayStatus,
    onFailure: () -> Unit,
    onSuccess: () -> Unit,
) {

    when (status) {
        is WxPayStatus.Loading -> {
            WxRespProgress(
                title = stringResource(id = R.string.wxpay_query_order),
                subTitle = "",
                buttonText = null,
                onClickButton = {}
            )
        }
        is WxPayStatus.Success -> {
            WxRespProgress(
                title = stringResource(id = R.string.payment_done),
                subTitle = "",
                buttonText = "查看结果",
                onClickButton = onSuccess,
            )
        }
        is WxPayStatus.Canceled -> {
            WxRespProgress(
                title = stringResource(id = R.string.wxpay_cancelled),
                subTitle = "",
                onClickButton = onFailure
            )
        }
        is WxPayStatus.Error -> {
            WxRespProgress(
                title = stringResource(id = R.string.wxpay_failed),
                subTitle = "Error: ${status.message}",
                onClickButton = onFailure
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWxPayScreenLoading() {
    WxPayScreen(
        status = WxPayStatus.Loading,
        onFailure = {},
        onSuccess = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewWxPayScreenSuccess() {
    WxPayScreen(
        status = WxPayStatus.Success,
        onFailure = {},
        onSuccess = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewWxPayScreenError() {
    WxPayScreen(
        status = WxPayStatus.Error("Unknown"),
        onFailure = {},
        onSuccess = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewWxPayScreenCanceled() {
    WxPayScreen(
        status = WxPayStatus.Canceled,
        onFailure = {},
        onSuccess = {}
    )
}
