package com.ft.ftchinese.ui.member

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.ListItemTwoCol
import com.ft.ftchinese.ui.product.ProductHeading
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun MemberScreen(
    member: Membership,
    isRefreshing: Boolean,
) {
    val context = LocalContext.current
    val status = SubsStatus.newInstance(
        ctx = context,
        m = member
    )

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = {

        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(Dimens.dp8)
        ) {
            SubsStatusCard(status = status)

            SubsOptions(
                cancelStripe = member.canCancelStripe,
                reactivateStripe = status.reactivateStripe,
                onClickRow = {

                }
            )
        }
    }
}

@Composable
fun SubsStatusCard(
    status: SubsStatus
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ProductHeading(text = status.productName)

            status.reminder?.let {
                RemindMessage(message = it)
            }

            status.details.forEach {
                ListItemTwoCol(
                    lead = it.first,
                    tail = it.second
                )
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

enum class OptionRow {
    GoToPaywall,
    CancelStripe,
    ReactivateStripe;
}

@Composable
fun SubsOptions(
    cancelStripe: Boolean,
    reactivateStripe: Boolean,
    onClickRow: (OptionRow) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ClickableRow(
            onClick = {
                onClickRow(OptionRow.GoToPaywall)
            }
        ) {
            Text(
                text = "购买订阅或更改自动续订"
            )
        }

        Divider()

        if (reactivateStripe) {
            ClickableRow(
                onClick = {
                    onClickRow(OptionRow.ReactivateStripe)
                }
            ) {
                Text(
                    text = stringResource(id = R.string.stripe_reactivate_auto_renew)
                )
            }

            Divider()
        }

        if (cancelStripe) {
            ClickableRow(onClick = {
                onClickRow(OptionRow.CancelStripe)
            }) {
                Text(
                    text = stringResource(id = R.string.stripe_cancel)
                )
            }

            Divider()
        }
    }
}
