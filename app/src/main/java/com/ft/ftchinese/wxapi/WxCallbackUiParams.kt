package com.ft.ftchinese.wxapi

import android.content.Context
import com.ft.ftchinese.R

data class WxCallbackUiParams(
    val title: String,
    val subTitle: String?,
    val button: String?,
)

fun buildWxPayUiParams(
    context: Context,
    status: WxPayStatus
): WxCallbackUiParams {
    return when (status) {
        is WxPayStatus.Loading -> WxCallbackUiParams(
            title = context.getString(R.string.wxpay_query_order),
            subTitle = null,
            button = null,
        )
        is WxPayStatus.Success -> WxCallbackUiParams(
            title = context.getString(R.string.payment_done),
            subTitle = null,
            button = context.getString(R.string.btn_done)
        )
        is WxPayStatus.Canceled -> WxCallbackUiParams(
            title = context.getString(R.string.wxpay_cancelled),
            subTitle = null,
            button = context.getString(R.string.btn_done)
        )
        is WxPayStatus.Error -> WxCallbackUiParams(
            title = context.getString(R.string.wxpay_failed),
            subTitle = "Error: ${status.message}",
            button = context.getString(R.string.btn_done)
        )
    }
}

