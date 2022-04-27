package com.ft.ftchinese.ui.member

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.theme.Dimens

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
    val rowModifier = Modifier.padding(vertical = Dimens.dp8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        ClickableRow(
            onClick = {
                onClickRow(OptionRow.GoToPaywall)
            },
            modifier = rowModifier
        ) {
            Text(
                text = "购买订阅或更改自动续订",
                modifier = Modifier.weight(1f)
            )
        }

        Divider()

        if (reactivateStripe) {
            ClickableRow(
                onClick = {
                    onClickRow(OptionRow.ReactivateStripe)
                },
                modifier = rowModifier
            ) {
                Text(
                    text = stringResource(id = R.string.stripe_reactivate_auto_renew)
                )
            }

            Divider()
        }

        if (cancelStripe) {
            ClickableRow(
                onClick = {
                    onClickRow(OptionRow.CancelStripe)
                },
                modifier = rowModifier
            ) {
                Text(
                    text = stringResource(id = R.string.stripe_cancel)
                )
            }

            Divider()
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
