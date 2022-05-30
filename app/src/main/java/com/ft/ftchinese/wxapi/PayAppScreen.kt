package com.ft.ftchinese.wxapi

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class PayAppScreen(
    @StringRes val titleId: Int,
) {
    PayResponse(titleId = R.string.pay_brand_wechat);

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): PayAppScreen =
            when (route?.substringBefore("/")) {
                PayResponse.name -> PayResponse
                null -> PayResponse
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
