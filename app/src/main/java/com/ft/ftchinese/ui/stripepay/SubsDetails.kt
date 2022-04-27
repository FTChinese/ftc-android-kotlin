package com.ft.ftchinese.ui.stripepay

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.StripeSubStatus
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.stripesubs.StripeSubs
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun SubsDetails(
    subs: StripeSubs
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(top = Dimens.dp16)
    ) {
        Text(
            text = "订阅成功",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )
        SubsDetailRow(
            lead = "订阅方案",
            tail = FormatHelper.getTier(context, subs.tier)
        )
        SubsDetailRow(
            lead = "订阅状态",
            tail = subs.status?.let {
                FormatHelper.getStripeSubsStatus(context, it)
            } ?: ""
        )
        SubsDetailRow(
            lead = "开始时间",
            tail = subs.currentPeriodStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        SubsDetailRow(
            lead = "结束时间",
            tail = subs.currentPeriodEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )

        Text(
            text = "本次订阅时间结束后自动续订",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body2,
            color = OColor.black60,
        )
    }
}

@Composable
private fun SubsDetailRow(
    lead: String,
    tail: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.dp8),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = lead,
            style = MaterialTheme.typography.body1
        )
        Text(
            text = tail,
            style = MaterialTheme.typography.body1,
            color = OColor.black80
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewSubsDetails() {
    SubsDetails(subs = StripeSubs(
        id = "",
        tier = Tier.STANDARD,
        cycle = Cycle.YEAR,
        cancelAtPeriodEnd = false,
        currentPeriodStart = ZonedDateTime.now(),
        currentPeriodEnd = ZonedDateTime.now().plusYears(1),
        customerId = "",
        latestInvoiceId = "",
        liveMode = false,
        status = StripeSubStatus.Active,
    ))
}
