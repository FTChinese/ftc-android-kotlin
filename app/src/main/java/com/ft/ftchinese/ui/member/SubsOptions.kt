package com.ft.ftchinese.ui.member

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.RightArrow
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor

enum class SubsOptionRow {
    GoToPaywall,
    CancelStripe,
    ReactivateStripe;
}

@Composable
fun SubsOptions(
    cancelStripe: Boolean,
    reactivateStripe: Boolean,
    onClickRow: (SubsOptionRow) -> Unit
) {
    val rowModifier = Modifier
        .background(OColor.black5)
        .padding(Dimens.dp8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        ClickableRow(
            modifier = rowModifier,
            endIcon = { RightArrow() },
            onClick = {
                onClickRow(SubsOptionRow.GoToPaywall)
            }
        ) {
            Text(
                text = "购买订阅或更改自动续订",
                color = OColor.teal
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        if (reactivateStripe) {
            ClickableRow(
                modifier = rowModifier,
                onClick = {
                    onClickRow(SubsOptionRow.ReactivateStripe)
                },
            ) {
                Text(
                    text = stringResource(id = R.string.stripe_reactivate_auto_renew)
                )
            }

            Spacer(modifier = Modifier.height(1.dp))
        }

        if (cancelStripe) {
            ClickableRow(
                modifier = rowModifier,
                onClick = {
                    onClickRow(SubsOptionRow.CancelStripe)
                }
            ) {
                Text(
                    text = stringResource(id = R.string.stripe_cancel)
                )
            }

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSubsOptions() {
    SubsOptions(
        cancelStripe = true,
        reactivateStripe = false,
        onClickRow = {}
    )
}
