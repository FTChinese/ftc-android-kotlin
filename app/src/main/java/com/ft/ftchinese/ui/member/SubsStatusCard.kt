package com.ft.ftchinese.ui.member

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.components.ListItemTwoCol
import com.ft.ftchinese.ui.product.ProductHeading
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun SubsStatusCard(
    status: SubsStatus
) {
    Card(
        modifier = Modifier.padding(Dimens.dp8),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
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
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(Dimens.dp8))
                RemindMessage(message = "以下订阅时间将在现有订阅服务到期后使用")
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
