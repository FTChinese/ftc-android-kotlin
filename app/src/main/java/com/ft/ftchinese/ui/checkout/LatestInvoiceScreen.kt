package com.ft.ftchinese.ui.checkout

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.Invoices
import com.ft.ftchinese.model.invoice.Invoice
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.theme.Dimens
import org.threeten.bp.ZonedDateTime

@Composable
fun LatestInvoiceScreen(
    invoice: Invoices,
    onClickNext: () -> Unit,
) {
    val context = LocalContext.current

    Column {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(Dimens.dp8)
        ) {
            InvoiceTable(data = invoiceRow(context, invoice.purchased))

            Spacer(modifier = Modifier.height(Dimens.dp16))

            invoice.carriedOver?.let {
                Text(
                    text = "购买前会员剩余 ${it.totalDays} 天，将在当前会员到期后继续使用",
                    modifier = Modifier.padding(Dimens.dp8)
                )
            }
        }

        PrimaryButton(
            onClick = onClickNext,
            modifier = Modifier
                .padding(Dimens.dp16)
                .fillMaxWidth()
        ) {
            Text(text = "下一步，确认或完善个人信息")
        }
    }
}

@Composable
private fun InvoiceTable(
    data: List<Pair<String, String>>
) {
    Column {
        data.forEach { pair ->
            Row(
                modifier = Modifier.padding(Dimens.dp8)
            ) {
                Text(
                    text = pair.first,
                    modifier = Modifier
                        .weight(1f)
                        .padding()
                )
                Text(
                    text = pair.second,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}

private fun invoiceRow(ctx: Context, inv: Invoice): List<Pair<String, String>> {
    return listOf(
        Pair(
            "订单号",
            inv.orderId ?: "",
        ),
        Pair(
            "订阅方案",
            FormatHelper.getTier(ctx, inv.tier),
        ),
        Pair(
            "支付金额",
            FormatHelper.currencySymbol(inv.currency) + FormatHelper.formatMoney(ctx, inv.paidAmount),
        ),
        Pair(
            "支付方式",
            inv.payMethod?.let {
                FormatHelper.getPayMethod(ctx, it)
            } ?: "",
        ),
        Pair(
            "订阅期限",
            if (inv.orderKind == OrderKind.AddOn) {
                when {
                    inv.years > 0 -> FormatHelper.getCycleN(ctx, Cycle.YEAR, inv.years)
                    inv.months > 0 -> FormatHelper.getCycleN(ctx, Cycle.MONTH, inv.months)
                    else -> FormatHelper.getCycleN(ctx, inv.period.toCycle(), 1)
                } + "(当前订阅过期后启用)"
            } else {
                "${inv.startUtc?.toLocalDate()} 至 ${inv.endUtc?.toLocalDate()}"
            }
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewLatestInvoiceScreen() {
    LatestInvoiceScreen(
        invoice = Invoices(
            purchased = Invoice(
                id = "FT1234567890",
                compoundId = "",
                tier = Tier.PREMIUM,
                cycle = Cycle.YEAR,
                years = 1,
                months = 0,
                days = 0,
                paidAmount = 1998.0,
                payMethod = PayMethod.ALIPAY,
                startUtc = ZonedDateTime.now(),
                endUtc = ZonedDateTime.now().plusYears(1)
            ),
            carriedOver = Invoice(
                id = "",
                compoundId = "",
                tier = Tier.STANDARD,
                cycle = Cycle.YEAR,
                years = 0,
                months = 0,
                days = 99,
                paidAmount = 0.0,
            )
        ),
        onClickNext = {}
    )
}
