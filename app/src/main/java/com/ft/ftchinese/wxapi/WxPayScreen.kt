package com.ft.ftchinese.wxapi

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.components.SecondaryButton
import com.ft.ftchinese.ui.member.SubsStatus
import com.ft.ftchinese.ui.member.SubsStatusCard
import com.ft.ftchinese.ui.theme.Dimens
import org.threeten.bp.LocalDate

@Composable
fun WxPayScreen(
    status: WxPayStatus,
    onDone: () -> Unit,
) {

    val context = LocalContext.current

    val uiText = buildWxPayUIParams(
        LocalContext.current,
        status
    )

    Column(
        modifier = Modifier
            .padding(Dimens.dp16)
            .fillMaxSize()
    ) {
        Text(
            text = uiText.title,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.h6
        )
        uiText.subTitle?.let {
            Spacer(modifier = Modifier.height(Dimens.dp16))
            Text(
                text = it,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.body1
            )
        }

        if (status is WxPayStatus.Success) {
            Spacer(modifier = Modifier.height(Dimens.dp16))
            SubsStatusCard(
                status = SubsStatus.newInstance(
                    ctx = context,
                    m = status.membership
                )
            )
        }

        uiText.button?.let {
            Spacer(modifier = Modifier.height(Dimens.dp16))
            SecondaryButton(
                onClick = onDone,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = it)
            }
        }
    }
}

data class WxPayUIParams(
    val title: String,
    val subTitle: String?,
    val button: String?,
)

fun buildWxPayUIParams(
    context: Context,
    status: WxPayStatus
): WxPayUIParams {
    return when (status) {
        is WxPayStatus.Loading -> WxPayUIParams(
            title = context.getString(R.string.wxpay_query_order),
            subTitle = null,
            button = null,
        )
        is WxPayStatus.Success -> WxPayUIParams(
            title = context.getString(R.string.payment_done),
            subTitle = null,
            button = context.getString(R.string.btn_done)
        )
        is WxPayStatus.Canceled -> WxPayUIParams(
            title = context.getString(R.string.wxpay_cancelled),
            subTitle = null,
            button = context.getString(R.string.btn_done)
        )
        is WxPayStatus.Error -> WxPayUIParams(
            title = context.getString(R.string.wxpay_failed),
            subTitle = "Error: ${status.message}",
            button = context.getString(R.string.btn_done)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWxPayScreen() {
    WxPayScreen(
        status = WxPayStatus.Success(Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            payMethod = PayMethod.WXPAY,
            expireDate = LocalDate.now().plusYears(1)
        ))
//        status = WxPayStatus.Error("Unknown")
    ) {

    }
}
