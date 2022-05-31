package com.ft.ftchinese.ui.subs.mysubs

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.components.ListItemTwoCol
import com.ft.ftchinese.ui.subs.product.ProductHeading
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun SubsStatusCard(
    status: SubsStatus
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.dp16)
        ) {
            ProductHeading(text = status.productName)

            status.reminder?.let {
                RemindMessage(message = it)
            }

            Spacer(modifier = Modifier.height(Dimens.dp8))

            status.details.forEach {
                ListItemTwoCol(
                    lead = it.first,
                    tail = it.second
                )
            }

            if (status.addOns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.dp16))

                Text(
                    text = "待启用订阅时长",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.subtitle1
                )
                Spacer(modifier = Modifier.height(Dimens.dp8))
                Text(
                    text = "以下订阅时间将在现有订阅服务到期后使用",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.subtitle2,
                    color = OColor.black60
                )
                Spacer(modifier = Modifier.height(Dimens.dp8))
                status.addOns.forEach {
                    ListItemTwoCol(
                        lead = it.first,
                        tail = it.second
                    )
                }
            }
        }
    }
}

@Composable
private fun RemindMessage(
    message: String
) {
    Text(
        text = message,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.body1,
        color = OColor.claret80
    )
}

@Preview
@Composable
fun PreviewSubsStatusCard() {
    SubsStatusCard(
        status = SubsStatus(
            reminder = "Will expire. Please renew",
            productName = "Standard Edition",
            details = listOf(
                Pair("Expires on", "2022-05-01")
            ),
            reactivateStripe = false,
            addOns = listOf(
                Pair("Standard", "30 days")
            )
        )
    )
}
