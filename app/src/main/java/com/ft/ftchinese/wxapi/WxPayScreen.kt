package com.ft.ftchinese.wxapi

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership
import org.threeten.bp.LocalDate

@Composable
fun WxPayScreen(
    status: WxPayStatus,
    onDone: () -> Unit,
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
                buttonText = stringResource(id = R.string.btn_done),
                onClickButton = onDone,
                subscribed = status.membership
            )
        }
        is WxPayStatus.Canceled -> {
            WxRespProgress(
                title = stringResource(id = R.string.wxpay_cancelled),
                subTitle = "",
                onClickButton = onDone
            )
        }
        is WxPayStatus.Error -> {
            WxRespProgress(
                title = stringResource(id = R.string.wxpay_failed),
                subTitle = "Error: ${status.message}",
                onClickButton = onDone
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWxPayScreenLoading() {
    WxPayScreen(
        status = WxPayStatus.Loading,
        onDone = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewWxPayScreenSuccess() {
    WxPayScreen(
        status = WxPayStatus.Success(Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            payMethod = PayMethod.WXPAY,
            expireDate = LocalDate.now().plusYears(1)
        )),
        onDone = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewWxPayScreenError() {
    WxPayScreen(
        status = WxPayStatus.Error("Unknown"),
        onDone = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewWxPayScreenCanceled() {
    WxPayScreen(
        status = WxPayStatus.Canceled,
        onDone = {}
    )
}
