package com.ft.ftchinese.ui.subs.member

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.ui.subs.StripeAutoRenewUiState
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.IconRightArrow
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColors

enum class SubsOptionRow {
    GoToPaywall,
    CancelStripe,
    ReactivateStripe;
}

@Composable
fun SubsOptions(
    stripeAutoRenewUiState: StripeAutoRenewUiState?,
    onStripeAutoRenewChange: (Boolean) -> Unit,
    onClickRow: (SubsOptionRow) -> Unit,
) {

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            ClickableRow(
                onClick = {
                    onClickRow(SubsOptionRow.GoToPaywall)
                },
                endIcon = {
                    IconRightArrow()
                },
                contentPadding = PaddingValues(Dimens.dp8)
            ) {
                Text(
                    text = if (stripeAutoRenewUiState == null) {
                        "购买订阅"
                    } else {
                        "购买订阅或更改方案"
                    }
                )
            }

            stripeAutoRenewUiState?.let { autoRenew ->
                Divider(startIndent = Dimens.dp8)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = autoRenew.title)
                        Text(
                            text = autoRenew.status,
                            color = OColors.black50Default,
                        )
                        Text(
                            text = autoRenew.detail,
                            color = OColors.black50Default,
                        )
                    }
                    Switch(
                        checked = autoRenew.checked,
                        onCheckedChange = onStripeAutoRenewChange,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSubsOptions() {
    SubsOptions(
        stripeAutoRenewUiState = StripeAutoRenewUiState(
            visible = true,
            checked = true,
            title = "信用卡/借记卡自动续订",
            status = "已开启 · 下次续订：高端会员",
            detail = "当前高端会员权益有效至2027-05-15，到期后将继续自动续订。",
            offConfirmation = "",
        ),
        onStripeAutoRenewChange = {},
        onClickRow = {}
    )
}
