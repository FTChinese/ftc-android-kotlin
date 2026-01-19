package com.ft.ftchinese.ui.subs.ftcpay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.ui.components.BodyText0
import com.ft.ftchinese.ui.components.CheckVariant
import com.ft.ftchinese.ui.components.OCheckbox
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun PaymentMethodsGroup(
    selected: PayMethod?,
    forbidden: Boolean,
    onSelect: (PayMethod) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        listOf(PayMethod.ALIPAY, PayMethod.WXPAY)
            .forEach { payMethod ->
                PayMethodItem(
                    method = payMethod,
                    selected = (payMethod == selected),
                    enabled = !forbidden,
                    onSelect = onSelect,
                )
            }
    }

}

@Composable
fun PayMethodItem(
    method: PayMethod,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (PayMethod) -> Unit,
) {

    val res = PaymentBrandRes.of(method) ?: return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(Dimens.dp8)
            .selectable(
                selected = selected,
                onClick = { onSelect(method) },
                enabled = enabled,
            )
            .fillMaxWidth(),
    ) {

        Image(
            painter = painterResource(id = res.drawableId),
            contentDescription = method.symbol,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(24.dp)
        )

        BodyText0(
            text = stringResource(id = res.stringId),
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.dp16),
            color = LocalContentColor.current.copy(
                if (enabled) {
                    LocalContentAlpha.current
                } else {
                    ContentAlpha.disabled
                }
            )
        )

        OCheckbox(
            checked = selected,
            onCheckedChange = { onSelect(method) },
            enabled = enabled,
            variant = CheckVariant.Circle,
        )

    }
}

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
            drawableId = R.drawable.ic_card_membership_black_24dp,
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

@Preview(showBackground = true)
@Composable
fun PreviewPayMethodItem_Enabled() {
    PayMethodItem(
        method = PayMethod.WXPAY,
        selected = true,
        enabled = true,
        onSelect = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPayMethodItem_Disabled() {
    PayMethodItem(
        method = PayMethod.WXPAY,
        selected = false,
        enabled = false,
        onSelect = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPaymentMethodGroup() {
    PaymentMethodsGroup(
        selected = PayMethod.ALIPAY,
        forbidden = false,
        onSelect = {}
    )
}
