package com.ft.ftchinese.ui.subs.stripepay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.StripeSubStatus
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.formatLocalDate
import com.ft.ftchinese.model.stripesubs.StripeCoupon
import com.ft.ftchinese.model.stripesubs.StripeSubs
import com.ft.ftchinese.ui.components.Heading3
import com.ft.ftchinese.ui.components.ListItemTwoCol
import com.ft.ftchinese.ui.components.SubHeading1
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.formatter.formatAmountOff
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import org.threeten.bp.ZonedDateTime

@Composable
fun StripeSubsDetails(
    subs: StripeSubs,
    coupon: StripeCoupon?
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(top = Dimens.dp16)
    ) {
        Heading3(
            text = "订阅成功",
            textAlign = TextAlign.Center,
        )

        if (coupon != null) {
            SubHeading1(
                text = stringResource(
                    R.string.coupon_redeemed,
                    formatAmountOff(context, coupon.moneyParts)
                )
            )
        }

        ListItemTwoCol(
            lead = "订阅方案",
            tail = FormatHelper.getTier(context, subs.tier)
        )
        ListItemTwoCol(
            lead = "订阅状态",
            tail = subs.status?.let {
                FormatHelper.getStripeSubsStatus(context, it)
            } ?: ""
        )
        ListItemTwoCol(
            lead = "开始时间",
            tail = formatLocalDate(subs.currentPeriodStart)
        )
        ListItemTwoCol(
            lead = "结束时间",
            tail = formatLocalDate(subs.currentPeriodEnd)
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


@Preview(showBackground = true)
@Composable
fun PreviewSubsDetails() {
    StripeSubsDetails(
        subs = StripeSubs(
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
        ),
        coupon = StripeCoupon(
            id = "coupon-id",
            amountOff = 10,
            currency = "gbp",
            redeemBy = 0,
            priceId = "attached-price-id",
            startUtc = ZonedDateTime.now(),
            endUtc = ZonedDateTime.now().plusDays(7)
        )
    )
}
