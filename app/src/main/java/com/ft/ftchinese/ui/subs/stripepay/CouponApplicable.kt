package com.ft.ftchinese.ui.subs.stripepay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.stripesubs.CouponApplied
import com.ft.ftchinese.model.stripesubs.StripeCoupon
import com.ft.ftchinese.ui.components.BodyText1
import com.ft.ftchinese.ui.components.BodyText2
import com.ft.ftchinese.ui.components.Heading3
import com.ft.ftchinese.ui.components.SubHeading1
import com.ft.ftchinese.ui.formatter.formatAmountOff
import com.ft.ftchinese.ui.formatter.formatCouponEnjoyed
import com.ft.ftchinese.ui.formatter.formatRedeemPeriod
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import org.threeten.bp.ZonedDateTime

@Composable
fun CouponApplicable(
    coupon: StripeCoupon,
    applied: CouponApplied?,
) {
    val context = LocalContext.current

    Card(
        elevation = Dimens.dp4
    ) {
        Column(
            modifier = Modifier.padding(Dimens.dp8)
        ) {
            SubHeading1(
                text = stringResource(id = R.string.title_coupon),
                textAlign = TextAlign.Center
            )
            Heading3(
                text = formatAmountOff(
                    context,
                    coupon.moneyParts
                )
            )

            Spacer(modifier = Modifier.height(Dimens.dp16))

            BodyText1(
                text = formatRedeemPeriod(
                    context,
                    coupon.startUtc,
                    coupon.endUtc
                ),
                textAlign = TextAlign.Center,
            )

            applied?.redeemedUtc?.let {
                Spacer(modifier = Modifier.height(Dimens.dp16))
                BodyText2(
                    text = formatCouponEnjoyed(
                        context,
                        it
                    ),
                    textAlign = TextAlign.Center,
                    color = OColor.claret
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewCouponApplicable() {
    CouponApplicable(
        coupon = StripeCoupon(
            id = "coupon-id",
            amountOff = 10,
            currency = "gbp",
            redeemBy = 0,
            priceId = "attached-price-id",
            startUtc = ZonedDateTime.now(),
            endUtc = ZonedDateTime.now().plusDays(7)
        ),
        applied = CouponApplied(
            invoiceId = "invoice-id",
            ftcId = "user-id",
            subsId = "subscription-id",
            couponId = "coupon-id",
            createdUtc = ZonedDateTime.now(),
            redeemedUtc = ZonedDateTime.now(),
        )
    )
}
