package com.ft.ftchinese.ui.subs.ftcpay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

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
            .selectable(
                selected = selected,
                onClick = { onSelect(method) },
                enabled = enabled,
            )
            .padding(Dimens.dp8)
            .fillMaxWidth(),
    ) {

        Image(
            painter = painterResource(id = res.drawableId),
            contentDescription = method.symbol,
        )

        Text(
            text = stringResource(id = res.stringId),
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.dp16),
        )

        IconToggleButton(
            checked = selected,
            onCheckedChange = { onSelect(method) },
            enabled = enabled,
        ) {
            Icon(
                painter = painterResource(
                    id = if (selected) {
                        R.drawable.ic_baseline_check_circle_outline_24
                    } else {
                        R.drawable.ic_baseline_radio_button_unchecked_24
                    }
                ),
                contentDescription = null,
                tint = OColor.teal
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPayMethodItem() {
    PayMethodItem(
        method = PayMethod.WXPAY,
        selected = true,
        enabled = true,
        onSelect = {}
    )
}
