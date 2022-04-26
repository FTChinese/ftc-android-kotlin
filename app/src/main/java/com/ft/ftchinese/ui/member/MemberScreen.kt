package com.ft.ftchinese.ui.member

import androidx.compose.foundation.layout.*
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
import com.ft.ftchinese.ui.product.SubsRuleContent
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

            SubsRuleContent()
        }
    }
}





