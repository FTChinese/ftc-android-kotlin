package com.ft.ftchinese.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.ui.theme.Dimens

data class PaymentBrandRes(
    val drawableId: Int,
    val stringId: Int,
) {
    companion object {
        val aliPay = PaymentBrandRes(
            drawableId = R.drawable.alipay,
            stringId = R.string.pay_brand_ali
        )

        val wxPay = PaymentBrandRes(
            drawableId = R.drawable.wechat_pay,
            stringId = R.string.pay_brand_wechat
        )

        val stripe = PaymentBrandRes(
            drawableId = R.drawable.stripe,
            stringId = R.string.pay_brand_stripe
        )

        @JvmStatic
        fun of(pm: PayMethod): PaymentBrandRes? {
            return when (pm) {
                PayMethod.ALIPAY -> aliPay
                PayMethod.WXPAY -> wxPay
                PayMethod.STRIPE -> stripe
                else -> null
            }
        }
    }
}

@Composable
fun PaymentBrand(
    payMethod: PayMethod,
) {
    val res = PaymentBrandRes.of(payMethod) ?: return
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = res.drawableId),
            contentDescription = payMethod.symbol,
        )

        Text(
            text = stringResource(id = res.stringId),
            modifier = Modifier.padding(start = Dimens.dp16),
        )
    }
}
